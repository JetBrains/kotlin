/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.AnnotationVisitor;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.context.FieldOwnerContext;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.descriptors.serialization.DescriptorSerializer;
import org.jetbrains.jet.descriptors.serialization.JavaProtoBufUtil;
import org.jetbrains.jet.descriptors.serialization.PackageData;
import org.jetbrains.jet.descriptors.serialization.ProtoBuf;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.asm4.Opcodes.*;
import static org.jetbrains.jet.descriptors.serialization.NameSerializationUtil.createNameResolver;
import static org.jetbrains.jet.lang.resolve.BindingContext.DECLARATION_TO_DESCRIPTOR;
import static org.jetbrains.jet.lang.resolve.BindingContextUtils.getNotNull;

public class NamespaceCodegen extends MemberCodegen {
    @NotNull
    private final ClassBuilderOnDemand v;
    @NotNull private final FqName name;
    private final Collection<JetFile> files;

    public NamespaceCodegen(
            @NotNull ClassBuilderOnDemand v,
            @NotNull final FqName fqName,
            GenerationState state,
            Collection<JetFile> namespaceFiles
    ) {
        super(state, null);
        checkAllFilesHaveSameNamespace(namespaceFiles);

        this.v = v;
        name = fqName;
        this.files = namespaceFiles;

        final PsiFile sourceFile = namespaceFiles.size() == 1 ? namespaceFiles.iterator().next().getContainingFile() : null;

        v.addOptionalDeclaration(new ClassBuilderOnDemand.ClassBuilderCallback() {
            @Override
            public void doSomething(@NotNull ClassBuilder v) {
                v.defineClass(sourceFile, V1_6,
                              ACC_PUBLIC | ACC_FINAL,
                              getJVMClassNameForKotlinNs(fqName).getInternalName(),
                              null,
                              //"jet/lang/Namespace",
                              "java/lang/Object",
                              new String[0]
                );
                //We don't generate any source information for namespace with multiple files
                if (sourceFile != null) {
                    v.visitSource(sourceFile.getName(), null);
                }
            }
        });
    }

    public void generate(@NotNull CompilationErrorHandler errorHandler) {
        if (shouldGenerateNSClass(files)) {
            AnnotationVisitor packageClassAnnotation =
                    v.getClassBuilder().newAnnotation(JvmStdlibNames.JET_PACKAGE_CLASS.getDescriptor(), true);
            packageClassAnnotation.visit(JvmStdlibNames.ABI_VERSION_NAME, JvmAbi.VERSION);
            packageClassAnnotation.visitEnd();
        }

        List<MemberMap> namespaceMembers = new ArrayList<MemberMap>();

        for (JetFile file : files) {
            try {
                ClassBuilder builder = generate(file);
                if (builder != null) {
                    namespaceMembers.add(builder.getMemberMap());
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

        writeKotlinInfoIfNeeded(MemberMap.union(namespaceMembers));

        assert v.isActivated() == shouldGenerateNSClass(files) : "Different algorithms for generating namespace class and for heuristics";
    }

    private void writeKotlinInfoIfNeeded(@NotNull MemberMap members) {
        DescriptorSerializer serializer = new DescriptorSerializer(new JavaSerializerExtension(members));
        ProtoBuf.Package.Builder packageProto = ProtoBuf.Package.newBuilder();
        boolean writeAnnotation = false;

        for (JetFile file : files) {
            for (JetDeclaration declaration : file.getDeclarations()) {
                if (declaration instanceof JetProperty || declaration instanceof JetNamedFunction) {
                    writeAnnotation = true;
                    DeclarationDescriptor descriptor = getNotNull(state.getBindingContext(), DECLARATION_TO_DESCRIPTOR, declaration);
                    ProtoBuf.Callable proto = serializer.callableProto((CallableMemberDescriptor) descriptor).build();
                    packageProto.addMember(proto);
                }
                else if (declaration instanceof JetClassOrObject) {
                    DeclarationDescriptor descriptor = getNotNull(state.getBindingContext(), DECLARATION_TO_DESCRIPTOR, declaration);
                    if (declaration instanceof JetObjectDeclaration) {
                        writeAnnotation = true;
                        JetObjectDeclarationName nameAsDeclaration = ((JetObjectDeclaration) declaration).getNameAsDeclaration();
                        assert nameAsDeclaration != null : "Should be a named object";
                        PropertyDescriptor propertyForObject = getNotNull(state.getBindingContext(), BindingContext.OBJECT_DECLARATION,
                                                                          nameAsDeclaration);
                        ProtoBuf.Callable proto = serializer.callableProto(propertyForObject).build();
                        packageProto.addMember(proto);
                    }
                    int name = serializer.getNameTable().getSimpleNameIndex(descriptor.getName());
                    packageProto.addClassName(name);
                }
            }
        }

        if (!writeAnnotation) return;

        PackageData data = new PackageData(createNameResolver(serializer.getNameTable()), packageProto.build());

        AnnotationVisitor av = v.getClassBuilder().newAnnotation(JvmStdlibNames.KOTLIN_INFO_CLASS.getDescriptor(), true);
        av.visit(JvmStdlibNames.ABI_VERSION_NAME, JvmAbi.VERSION);
        AnnotationVisitor array = av.visitArray(JvmStdlibNames.KOTLIN_INFO_DATA_FIELD);
        for (String string : JavaProtoBufUtil.encodeBytes(data.toBytes())) {
            array.visit(null, string);
        }
        array.visitEnd();
        av.visitEnd();
    }

    @Nullable
    private ClassBuilder generate(@NotNull JetFile file) {
        NamespaceDescriptor descriptor = state.getBindingContext().get(BindingContext.FILE_TO_NAMESPACE, file);
        assert descriptor != null : "No namespace found for file " + file + " declared package: " + file.getPackageName();
        boolean generateSrcClass = false;
        for (JetDeclaration declaration : file.getDeclarations()) {
            if (declaration instanceof JetProperty || declaration instanceof JetNamedFunction) {
                generateSrcClass = true;
            }
            else if (declaration instanceof JetClassOrObject) {
                if (state.isGenerateDeclaredClasses()) {
                    generateClassOrObject(descriptor, (JetClassOrObject) declaration);
                }
            }
            else if (declaration instanceof JetScript) {
                state.getScriptCodegen().generate((JetScript) declaration);
            }
        }

        if (!generateSrcClass) return null;

        String namespaceInternalName = JvmClassName.byFqNameWithoutInnerClasses(
                PackageClassUtils.getPackageClassFqName(name)).getInternalName();
        String className = getMultiFileNamespaceInternalName(namespaceInternalName, file);
        ClassBuilder builder = state.getFactory().forNamespacepart(className, file);

        builder.defineClass(file, V1_6,
                            ACC_PUBLIC | ACC_FINAL,
                            className,
                            null,
                            //"jet/lang/Namespace",
                            "java/lang/Object",
                            new String[0]
        );
        builder.visitSource(file.getName(), null);

        FieldOwnerContext nameSpaceContext = CodegenContext.STATIC.intoNamespace(descriptor);

        FieldOwnerContext nameSpacePart = CodegenContext.STATIC.intoNamespacePart(className, descriptor);

        for (JetDeclaration declaration : file.getDeclarations()) {
            if (declaration instanceof JetNamedFunction || declaration instanceof JetProperty) {
                genFunctionOrProperty(nameSpaceContext, (JetTypeParameterListOwner) declaration, builder);
                genFunctionOrProperty(nameSpacePart, (JetTypeParameterListOwner) declaration, v.getClassBuilder());
            }
        }

        generateStaticInitializers(descriptor, builder, file, nameSpaceContext);

        builder.done();

        return builder;
    }

    public void generateClassOrObject(@NotNull NamespaceDescriptor descriptor, @NotNull JetClassOrObject classOrObject) {
        CodegenContext context = CodegenContext.STATIC.intoNamespace(descriptor);
        genClassOrObject(context, classOrObject);
    }

    /**
     * @param namespaceFiles all files should have same package name
     * @return
     */
    public static boolean shouldGenerateNSClass(Collection<JetFile> namespaceFiles) {
        checkAllFilesHaveSameNamespace(namespaceFiles);

        for (JetFile file : namespaceFiles) {
            for (JetDeclaration declaration : file.getDeclarations()) {
                if (declaration instanceof JetProperty ||
                    declaration instanceof JetNamedFunction ||
                    declaration instanceof JetObjectDeclaration) {
                    return true;
                }
            }
        }

        return false;
    }

    private static void checkAllFilesHaveSameNamespace(Collection<JetFile> namespaceFiles) {
        FqName commonFqName = null;
        for (JetFile file : namespaceFiles) {
            FqName fqName = JetPsiUtil.getFQName(file);
            if (commonFqName != null) {
                if (!commonFqName.equals(fqName)) {
                    throw new IllegalArgumentException("All files should have same package name");
                }
            }
            else {
                commonFqName = JetPsiUtil.getFQName(file);
            }
        }
    }

    private void generateStaticInitializers(
            NamespaceDescriptor descriptor,
            @NotNull ClassBuilder builder,
            @NotNull JetFile file,
            @NotNull FieldOwnerContext context
    ) {
        List<JetProperty> properties = collectPropertiesToInitialize(file);
        if (properties.isEmpty()) return;

        MethodVisitor mv = builder.newMethod(file, ACC_STATIC, "<clinit>", "()V", null, null);
        if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            mv.visitCode();

            FrameMap frameMap = new FrameMap();

            SimpleFunctionDescriptorImpl clInit =
                    new SimpleFunctionDescriptorImpl(descriptor, Collections.<AnnotationDescriptor>emptyList(),
                                                     Name.special("<clinit>"),
                                                     CallableMemberDescriptor.Kind.SYNTHESIZED);
            clInit.initialize(null, null, Collections.<TypeParameterDescriptor>emptyList(),
                              Collections.<ValueParameterDescriptor>emptyList(), null, null, Visibilities.PRIVATE, false);

            ExpressionCodegen codegen = new ExpressionCodegen(mv, frameMap, Type.VOID_TYPE, context.intoFunction(clInit), state);

            for (JetDeclaration declaration : properties) {
                ImplementationBodyCodegen.
                        initializeProperty(codegen, state.getBindingContext(), (JetProperty) declaration);
            }

            mv.visitInsn(RETURN);
            FunctionCodegen.endVisit(mv, "static initializer for namespace", file);
            mv.visitEnd();
        }
    }

    @NotNull
    private List<JetProperty> collectPropertiesToInitialize(@NotNull JetFile file) {
        List<JetProperty> result = Lists.newArrayList();
        for (JetDeclaration declaration : file.getDeclarations()) {
            if (declaration instanceof JetProperty &&
                ImplementationBodyCodegen.shouldInitializeProperty((JetProperty) declaration, typeMapper)) {
                result.add((JetProperty) declaration);
            }
        }
        return result;
    }

    public void done() {
        v.done();
    }

    @NotNull
    public static JvmClassName getJVMClassNameForKotlinNs(@NotNull FqName fqName) {
        String packageClassName = PackageClassUtils.getPackageClassName(fqName);
        if (fqName.isRoot()) {
            return JvmClassName.byInternalName(packageClassName);
        }

        return JvmClassName.byFqNameWithoutInnerClasses(fqName.child(Name.identifier(packageClassName)));
    }

    @NotNull
    private static String getMultiFileNamespaceInternalName(@NotNull String namespaceInternalName, @NotNull PsiFile file) {
        String fileName = FileUtil.getNameWithoutExtension(PathUtil.getFileName(file.getName()));

        // path hashCode to prevent same name / different path collision
        return namespaceInternalName + "$src$" + replaceSpecialSymbols(fileName) + "$" + Integer.toHexString(
                CodegenUtil.getPathHashCode(file));
    }

    private static String replaceSpecialSymbols(@NotNull String str) {
        return str.replace('.', '_');
    }

    @NotNull
    public static String getNamespacePartInternalName(@NotNull JetFile file) {
        FqName fqName = JetPsiUtil.getFQName(file);
        JvmClassName namespaceJvmClassName = NamespaceCodegen.getJVMClassNameForKotlinNs(fqName);
        String namespaceInternalName = namespaceJvmClassName.getInternalName();
        return NamespaceCodegen.getMultiFileNamespaceInternalName(namespaceInternalName, file);
    }
}