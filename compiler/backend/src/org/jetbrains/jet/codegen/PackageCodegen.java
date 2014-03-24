/*
 * Copyright 2010-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.codegen;

import com.google.common.collect.Sets;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.AnnotationVisitor;
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.context.FieldOwnerContext;
import org.jetbrains.jet.codegen.context.PackageContext;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.GenerationStateAware;
import org.jetbrains.jet.descriptors.serialization.BitEncoding;
import org.jetbrains.jet.descriptors.serialization.DescriptorSerializer;
import org.jetbrains.jet.descriptors.serialization.PackageData;
import org.jetbrains.jet.descriptors.serialization.ProtoBuf;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.*;

import static org.jetbrains.asm4.Opcodes.*;
import static org.jetbrains.jet.codegen.AsmUtil.asmDescByFqNameWithoutInnerClasses;
import static org.jetbrains.jet.codegen.AsmUtil.asmTypeByFqNameWithoutInnerClasses;
import static org.jetbrains.jet.descriptors.serialization.NameSerializationUtil.createNameResolver;
import static org.jetbrains.jet.lang.resolve.java.PackageClassUtils.getPackageClassFqName;

public class PackageCodegen extends GenerationStateAware {
    @NotNull
    private final ClassBuilderOnDemand v;

    @NotNull
    private final FqName name;

    @NotNull
    private final Collection<JetFile> files;
    private final Set<PackageFragmentDescriptor> packageFragments;

    public PackageCodegen(
            @NotNull ClassBuilderOnDemand v,
            @NotNull final FqName fqName,
            @NotNull GenerationState state,
            @NotNull Collection<JetFile> packageFiles
    ) {
        super(state);
        checkAllFilesHaveSamePackage(packageFiles);

        this.v = v;
        name = fqName;
        this.files = packageFiles;

        packageFragments = Sets.newHashSet();
        for (JetFile file : packageFiles) {
            packageFragments.add(getPackageFragment(file));
        }

        final PsiFile sourceFile = packageFiles.size() == 1 ? packageFiles.iterator().next().getContainingFile() : null;

        v.addOptionalDeclaration(new ClassBuilderOnDemand.ClassBuilderCallback() {
            @Override
            public void doSomething(@NotNull ClassBuilder v) {
                v.defineClass(sourceFile, V1_6,
                              ACC_PUBLIC | ACC_FINAL,
                              JvmClassName.byFqNameWithoutInnerClasses(getPackageClassFqName(fqName)).getInternalName(),
                              null,
                              "java/lang/Object",
                              ArrayUtil.EMPTY_STRING_ARRAY
                );
                //We don't generate any source information for package with multiple files
                if (sourceFile != null) {
                    v.visitSource(sourceFile.getName(), null);
                }
            }
        });
    }

    public void generate(@NotNull CompilationErrorHandler errorHandler) {
        List<JvmSerializationBindings> bindings = new ArrayList<JvmSerializationBindings>(files.size() + 1);
        boolean shouldGeneratePackageClass = shouldGeneratePackageClass(files);
        if (shouldGeneratePackageClass) {
            bindings.add(v.getClassBuilder().getSerializationBindings());
        }

        for (JetFile file : files) {
            try {
                ClassBuilder builder = generate(file);
                if (builder != null) {
                    bindings.add(builder.getSerializationBindings());
                }
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (Throwable e) {
                VirtualFile vFile = file.getVirtualFile();
                errorHandler.reportException(e, vFile == null ? "no file" : vFile.getUrl());
                DiagnosticUtils.throwIfRunningOnServer(e);
                if (ApplicationManager.getApplication().isInternal()) {
                    //noinspection CallToPrintStackTrace
                    e.printStackTrace();
                }
            }
        }

        if (shouldGeneratePackageClass) {
            writeKotlinPackageAnnotationIfNeeded(JvmSerializationBindings.union(bindings));
        }

        assert v.isActivated() == shouldGeneratePackageClass :
                "Different algorithms for generating package class and for heuristics for: " + name.asString();
    }

    private void writeKotlinPackageAnnotationIfNeeded(@NotNull JvmSerializationBindings bindings) {
        if (state.getClassBuilderMode() != ClassBuilderMode.FULL) {
            return;
        }

        // SCRIPT: Do not write annotations for scripts (if any is??)
        for (JetFile file : files) {
            if (file.isScript()) return;
        }

        DescriptorSerializer serializer = new DescriptorSerializer(new JavaSerializerExtension(bindings));
        ProtoBuf.Package packageProto = serializer.packageProto(packageFragments).build();

        if (packageProto.getMemberCount() == 0) return;

        PackageData data = new PackageData(createNameResolver(serializer.getNameTable()), packageProto);

        AnnotationVisitor av =
                v.getClassBuilder().newAnnotation(asmDescByFqNameWithoutInnerClasses(JvmAnnotationNames.KOTLIN_PACKAGE), true);
        av.visit(JvmAnnotationNames.ABI_VERSION_FIELD_NAME, JvmAbi.VERSION);
        AnnotationVisitor array = av.visitArray(JvmAnnotationNames.DATA_FIELD_NAME);
        for (String string : BitEncoding.encodeBytes(data.toBytes())) {
            array.visit(null, string);
        }
        array.visitEnd();
        av.visitEnd();
    }

    @Nullable
    private ClassBuilder generate(@NotNull JetFile file) {
        boolean generateSrcClass = false;
        Type packagePartType = getPackagePartType(getPackageClassFqName(name), file.getVirtualFile());
        PackageContext packagePartContext = CodegenContext.STATIC.intoPackagePart(getPackageFragment(file), packagePartType);

        for (JetDeclaration declaration : file.getDeclarations()) {
            if (declaration instanceof JetProperty || declaration instanceof JetNamedFunction) {
                generateSrcClass = true;
            }
            else if (declaration instanceof JetClassOrObject) {
                JetClassOrObject classOrObject = (JetClassOrObject) declaration;
                if (state.getGenerateDeclaredClassFilter().shouldProcess(classOrObject)) {
                    generateClassOrObject(classOrObject);
                }
            }
            else if (declaration instanceof JetScript) {
               // SCRIPT: generate script code, should be separate execution branch
               ScriptCodegen.createScriptCodegen((JetScript) declaration, state, packagePartContext).generate();
            }
        }

        if (!generateSrcClass) return null;

        ClassBuilder builder = state.getFactory().forPackagePart(packagePartType, file);

        new PackagePartCodegen(builder, file, packagePartType, packagePartContext, state).generate();

        FieldOwnerContext packageFacade = CodegenContext.STATIC.intoPackageFacade(packagePartType, getPackageFragment(file));
        //TODO: FIX: Default method generated at facade without delegation
        MemberCodegen memberCodegen = new MemberCodegen(state, null, packageFacade, null) {
            @NotNull
            @Override
            public ClassBuilder getBuilder() {
                return v.getClassBuilder();
            }
        };
        for (JetDeclaration declaration : file.getDeclarations()) {
            if (declaration instanceof JetNamedFunction || declaration instanceof JetProperty) {
                memberCodegen.genFunctionOrProperty(packageFacade, (JetTypeParameterListOwner) declaration, v.getClassBuilder());
            }
        }

        return builder;
    }

    @NotNull
    private PackageFragmentDescriptor getPackageFragment(@NotNull JetFile file) {
        PackageFragmentDescriptor packageFragment = bindingContext.get(BindingContext.FILE_TO_PACKAGE_FRAGMENT, file);
        assert packageFragment != null : "package fragment is null for " + file;
        return packageFragment;
    }

    public void generateClassOrObject(@NotNull JetClassOrObject classOrObject) {
        JetFile file = (JetFile) classOrObject.getContainingFile();
        Type packagePartType = getPackagePartType(getPackageClassFqName(name), file.getVirtualFile());
        CodegenContext context = CodegenContext.STATIC.intoPackagePart(getPackageFragment(file), packagePartType);
        MemberCodegen.genClassOrObject(context, classOrObject, state, null);
    }

    /**
     * @param packageFiles all files should have same package name
     * @return
     */
    public static boolean shouldGeneratePackageClass(@NotNull Collection<JetFile> packageFiles) {
        checkAllFilesHaveSamePackage(packageFiles);

        for (JetFile file : packageFiles) {
            for (JetDeclaration declaration : file.getDeclarations()) {
                if (declaration instanceof JetProperty || declaration instanceof JetNamedFunction) {
                    return true;
                }
            }
        }

        return false;
    }

    private static void checkAllFilesHaveSamePackage(Collection<JetFile> packageFiles) {
        FqName commonFqName = null;
        for (JetFile file : packageFiles) {
            FqName fqName = file.getPackageFqName();
            if (commonFqName != null) {
                if (!commonFqName.equals(fqName)) {
                    throw new IllegalArgumentException("All files should have same package name");
                }
            }
            else {
                commonFqName = file.getPackageFqName();
            }
        }
    }

    public void done() {
        v.done();
    }

    @NotNull
    public static Type getPackagePartType(@NotNull FqName facadeFqName, @NotNull VirtualFile file) {
        String fileName = FileUtil.getNameWithoutExtension(PathUtil.getFileName(file.getName()));

        // path hashCode to prevent same name / different path collision
        String srcName = facadeFqName.shortName().asString() + "-" + replaceSpecialSymbols(fileName) + "-" + Integer.toHexString(
                CodegenUtil.getPathHashCode(file));

        FqName srcFqName = facadeFqName.parent().child(Name.identifier(srcName));

        return asmTypeByFqNameWithoutInnerClasses(srcFqName);
    }

    @NotNull
    private static String replaceSpecialSymbols(@NotNull String str) {
        return str.replace('.', '_');
    }

    @NotNull
    public static String getPackagePartInternalName(@NotNull JetFile file) {
        FqName packageFqName = file.getPackageFqName();
        return getPackagePartType(getPackageClassFqName(packageFqName), file.getVirtualFile()).getInternalName();
    }
}
