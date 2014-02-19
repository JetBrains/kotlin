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

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.PathUtil;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.AnnotationVisitor;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.context.FieldOwnerContext;
import org.jetbrains.jet.codegen.context.MethodContext;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.codegen.context.PackageContext;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.descriptors.serialization.*;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedCallableMemberDescriptor;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedPropertyDescriptor;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedSimpleFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.codegen.state.GenerationStateAware;
import org.jetbrains.jet.descriptors.serialization.BitEncoding;
import org.jetbrains.jet.descriptors.serialization.DescriptorSerializer;
import org.jetbrains.jet.descriptors.serialization.PackageData;
import org.jetbrains.jet.descriptors.serialization.ProtoBuf;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.MemberComparator;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.java.lazy.descriptors.LazyJavaPackageFragmentScope;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.*;

import static org.jetbrains.asm4.Opcodes.*;
import static org.jetbrains.jet.codegen.AsmUtil.asmDescByFqNameWithoutInnerClasses;
import static org.jetbrains.jet.codegen.AsmUtil.asmTypeByFqNameWithoutInnerClasses;
import static org.jetbrains.jet.codegen.JvmSerializationBindings.METHOD_FOR_FUNCTION;
import static org.jetbrains.jet.descriptors.serialization.NameSerializationUtil.createNameResolver;
import static org.jetbrains.jet.lang.resolve.java.PackageClassUtils.getPackageClassFqName;

public class PackageCodegen extends GenerationStateAware {
    @NotNull
    private final ClassBuilderOnDemand v;

    @NotNull
    private final Collection<JetFile> files;
    private final PackageFragmentDescriptor packageFragment;
    private final PackageFragmentDescriptor compiledPackageFragment;

    public PackageCodegen(
            @NotNull ClassBuilderOnDemand v,
            @NotNull final FqName fqName,
            @NotNull GenerationState state,
            @NotNull Collection<JetFile> packageFiles
    ) {
        super(state);

        this.v = v;
        this.files = packageFiles;
        this.packageFragment = getOnlyPackageFragment();
        this.compiledPackageFragment = getCompiledPackageFragment();

        final PsiFile sourceFile = packageFiles.size() == 1 && getAlreadyCompiledCallables().isEmpty()
                                   ? packageFiles.iterator().next().getContainingFile() : null;

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

    @Nullable
    private PackageFragmentDescriptor getCompiledPackageFragment() {
        // TODO rewrite it to something more robust when module system is implemented
        for (PackageFragmentDescriptor anotherFragment : packageFragment.getContainingDeclaration().getPackageFragmentProvider()
                .getPackageFragments(packageFragment.getFqName())) {
            if (anotherFragment.getMemberScope() instanceof LazyJavaPackageFragmentScope) {
                return anotherFragment;
            }
        }
        return null;
    }

    @NotNull
    private List<DeserializedCallableMemberDescriptor> getAlreadyCompiledCallables() {
        List<DeserializedCallableMemberDescriptor> callables = Lists.newArrayList();
        if (compiledPackageFragment != null) {
            for (DeclarationDescriptor member : compiledPackageFragment.getMemberScope().getAllDescriptors()) {
                if (member instanceof DeserializedCallableMemberDescriptor) {
                    callables.add((DeserializedCallableMemberDescriptor) member);
                }
            }
        }
        return callables;
    }

    private void generateDelegationsToAlreadyCompiled(Map<CallableMemberDescriptor, Runnable> generateCallableMemberTasks) {
        for (final DeserializedCallableMemberDescriptor member : getAlreadyCompiledCallables()) {
            generateCallableMemberTasks.put(member, new Runnable() {
                @Override
                public void run() {
                    FieldOwnerContext context = CodegenContext.STATIC.intoPackageFacade(
                            Type.getObjectType(getPackagePartInternalName(member)),
                            compiledPackageFragment);

                    FunctionCodegen functionCodegen = new FunctionCodegen(
                            context,
                            v.getClassBuilder(),
                            state,
                            getMemberCodegen(context)
                    );

                    if (member instanceof DeserializedSimpleFunctionDescriptor) {
                        DeserializedSimpleFunctionDescriptor function = (DeserializedSimpleFunctionDescriptor) member;
                        JvmMethodSignature signature = typeMapper.mapSignature(function, OwnerKind.PACKAGE);
                        functionCodegen.generateMethod(null, signature, function,
                                                       new FunctionGenerationStrategy() {
                                                           @Override
                                                           public void generateBody(
                                                                   @NotNull MethodVisitor mv,
                                                                   @NotNull JvmMethodSignature signature,
                                                                   @NotNull MethodContext context,
                                                                   @Nullable MemberCodegen parentCodegen
                                                           ) {
                                                               throw new IllegalStateException("shouldn't be called");
                                                           }
                                                       });

                        v.getClassBuilder().getSerializationBindings().put(METHOD_FOR_FUNCTION, function, signature.getAsmMethod());
                    }
                    else if (member instanceof DeserializedPropertyDescriptor) {
                        PropertyCodegen propertyCodegen = new PropertyCodegen(
                                context, v.getClassBuilder(), functionCodegen, getMemberCodegen(context));
                        propertyCodegen.generateInPackageFacade((DeserializedPropertyDescriptor) member);
                    }
                    else {
                        throw new IllegalStateException("Unexpected member: " + member);
                    }
                }
            });
        }
    }

    public void generate(@NotNull CompilationErrorHandler errorHandler) {
        List<JvmSerializationBindings> bindings = new ArrayList<JvmSerializationBindings>(files.size() + 1);
        boolean shouldGeneratePackageClass = shouldGeneratePackageClass(files);
        if (shouldGeneratePackageClass) {
            bindings.add(v.getClassBuilder().getSerializationBindings());
        }

        Map<CallableMemberDescriptor, Runnable> generateCallableMemberTasks = new HashMap<CallableMemberDescriptor, Runnable>();

        for (JetFile file : files) {
            try {
                ClassBuilder builder = generate(file, generateCallableMemberTasks);
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
            // Shouldn't generate delegations to previously compiled if we compile only "classes" part of a package.
            generateDelegationsToAlreadyCompiled(generateCallableMemberTasks);
        }

        for (CallableMemberDescriptor member : Ordering.from(MemberComparator.INSTANCE).sortedCopy(generateCallableMemberTasks.keySet())) {
            generateCallableMemberTasks.get(member).run();
        }

        if (shouldGeneratePackageClass) {
            writeKotlinPackageAnnotationIfNeeded(JvmSerializationBindings.union(bindings));
        }

        assert v.isActivated() == shouldGeneratePackageClass :
                "Different algorithms for generating package class and for heuristics for: " + packageFragment;
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
        Collection<PackageFragmentDescriptor> packageFragments = compiledPackageFragment == null
                                                                 ? Collections.singleton(packageFragment)
                                                                 : Arrays.asList(packageFragment, compiledPackageFragment);
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
    private ClassBuilder generate(@NotNull JetFile file, @NotNull Map<CallableMemberDescriptor, Runnable> generateCallableMemberTasks) {
        boolean generateSrcClass = false;
        Type packagePartType = getPackagePartType(getPackageClassFqName(packageFragment.getFqName()), file.getVirtualFile());
        PackageContext packagePartContext = CodegenContext.STATIC.intoPackagePart(packageFragment, packagePartType);

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

        final FieldOwnerContext packageFacade = CodegenContext.STATIC.intoPackageFacade(packagePartType, packageFragment);
        
        final MemberCodegen memberCodegen = getMemberCodegen(packageFacade);

        for (final JetDeclaration declaration : file.getDeclarations()) {
            if (declaration instanceof JetNamedFunction || declaration instanceof JetProperty) {
                DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration);
                assert descriptor instanceof CallableMemberDescriptor :
                        "Expected callable member, was " + descriptor + " for " + declaration.getText();
                generateCallableMemberTasks.put(
                        (CallableMemberDescriptor) descriptor,
                        new Runnable() {
                            @Override
                            public void run() {
                                memberCodegen.genFunctionOrProperty(
                                        packageFacade, (JetTypeParameterListOwner) declaration, v.getClassBuilder());
                            }
                        }
                );
            }
        }

        return builder;
    }

    //TODO: FIX: Default method generated at facade without delegation
    private MemberCodegen getMemberCodegen(@NotNull FieldOwnerContext packageFacade) {
        return new MemberCodegen(state, null, packageFacade, null) {
            @NotNull
            @Override
            public ClassBuilder getBuilder() {
                return v.getClassBuilder();
            }
        };
    }

    @NotNull
    private PackageFragmentDescriptor getOnlyPackageFragment() {
        SmartList<PackageFragmentDescriptor> fragments = new SmartList<PackageFragmentDescriptor>();
        for (JetFile file : files) {
            PackageFragmentDescriptor fragment = bindingContext.get(BindingContext.FILE_TO_PACKAGE_FRAGMENT, file);
            assert fragment != null : "package fragment is null for " + file;

            if (!fragments.contains(fragment)) {
                fragments.add(fragment);
            }
        }
        if (fragments.size() != 1) {
            throw new IllegalStateException("More than one package fragment, files: " + files + " | fragments: " + fragments);
        }
        return fragments.get(0);
    }

    public void generateClassOrObject(@NotNull JetClassOrObject classOrObject) {
        JetFile file = (JetFile) classOrObject.getContainingFile();
        Type packagePartType = getPackagePartType(getPackageClassFqName(packageFragment.getFqName()), file.getVirtualFile());
        CodegenContext context = CodegenContext.STATIC.intoPackagePart(packageFragment, packagePartType);
        MemberCodegen.genClassOrObject(context, classOrObject, state, null);
    }

    /**
     * @param packageFiles all files should have same package name
     * @return
     */
    public static boolean shouldGeneratePackageClass(@NotNull Collection<JetFile> packageFiles) {
        for (JetFile file : packageFiles) {
            for (JetDeclaration declaration : file.getDeclarations()) {
                if (declaration instanceof JetProperty || declaration instanceof JetNamedFunction) {
                    return true;
                }
            }
        }

        return false;
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

        return getPackagePartType(facadeFqName, Name.identifier(srcName));
    }

    public static Type getPackagePartType(FqName facadeFqName, Name packagePartName) {
        return asmTypeByFqNameWithoutInnerClasses(facadeFqName.parent().child(packagePartName));
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

    @NotNull
    public static String getPackagePartInternalName(@NotNull DeserializedCallableMemberDescriptor deserializedCallable) {
        DeclarationDescriptor parent = deserializedCallable.getContainingDeclaration();
        assert parent instanceof PackageFragmentDescriptor : "parent should be package, but was: " + parent;

        assert deserializedCallable.getProto().hasExtension(JavaProtoBuf.implClassName)
                : "implClassName extension is absent for " + deserializedCallable;
        Name shortName = deserializedCallable.getNameResolver()
                .getName(deserializedCallable.getProto().getExtension(JavaProtoBuf.implClassName));
        FqName packagePartFqName = ((PackageFragmentDescriptor) parent).getFqName().child(shortName);
        return JvmClassName.byFqNameWithoutInnerClasses(packagePartFqName).getInternalName();
    }
}
