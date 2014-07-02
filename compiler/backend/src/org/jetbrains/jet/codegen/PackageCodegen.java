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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import kotlin.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.context.FieldOwnerContext;
import org.jetbrains.jet.codegen.context.MethodContext;
import org.jetbrains.jet.codegen.context.PackageContext;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.config.IncrementalCompilation;
import org.jetbrains.jet.descriptors.serialization.BitEncoding;
import org.jetbrains.jet.descriptors.serialization.DescriptorSerializer;
import org.jetbrains.jet.descriptors.serialization.PackageData;
import org.jetbrains.jet.descriptors.serialization.ProtoBuf;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedCallableMemberDescriptor;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedPropertyDescriptor;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedSimpleFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.MemberComparator;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames;
import org.jetbrains.jet.lang.resolve.java.jvmSignature.JvmMethodSignature;
import org.jetbrains.jet.lang.resolve.kotlin.PackagePartClassUtils;
import org.jetbrains.jet.lang.resolve.kotlin.incremental.IncrementalPackageFragmentProvider;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.org.objectweb.asm.AnnotationVisitor;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.util.*;

import static org.jetbrains.jet.codegen.AsmUtil.asmDescByFqNameWithoutInnerClasses;
import static org.jetbrains.jet.codegen.AsmUtil.method;
import static org.jetbrains.jet.descriptors.serialization.NameSerializationUtil.createNameResolver;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.K_PACKAGE_IMPL_TYPE;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.getType;
import static org.jetbrains.jet.lang.resolve.java.PackageClassUtils.getPackageClassFqName;
import static org.jetbrains.jet.lang.resolve.java.diagnostics.DiagnosticsPackage.*;
import static org.jetbrains.jet.lang.resolve.java.diagnostics.JvmDeclarationOrigin.NO_ORIGIN;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class PackageCodegen {
    private final ClassBuilderOnDemand v;
    private final GenerationState state;
    private final Collection<JetFile> files;
    private final Type packageClassType;
    private final PackageFragmentDescriptor packageFragment;
    private final PackageFragmentDescriptor compiledPackageFragment;
    private final List<DeserializedCallableMemberDescriptor> previouslyCompiledCallables;

    public PackageCodegen(@NotNull GenerationState state, @NotNull Collection<JetFile> files, @NotNull FqName fqName) {
        this.state = state;
        this.files = files;
        this.packageFragment = getOnlyPackageFragment(fqName);
        this.packageClassType = AsmUtil.asmTypeByFqNameWithoutInnerClasses(getPackageClassFqName(fqName));
        this.compiledPackageFragment = getCompiledPackageFragment(fqName);
        this.previouslyCompiledCallables = filterDeserializedCallables(compiledPackageFragment);

        assert packageFragment != null || compiledPackageFragment != null : fqName.asString() + " " + files;

        this.v = new ClassBuilderOnDemand(new Function0<ClassBuilder>() {
            @Override
            public ClassBuilder invoke() {
                Collection<JetFile> files = PackageCodegen.this.files;
                JetFile sourceFile = getRepresentativePackageFile(files);

                ClassBuilder v = PackageCodegen.this.state.getFactory().newVisitor(
                        PackageFacade(packageFragment == null ? compiledPackageFragment : packageFragment),
                        packageClassType, PackagePartClassUtils.getPackageFilesWithCallables(files)
                );
                v.defineClass(sourceFile, V1_6,
                              ACC_PUBLIC | ACC_FINAL,
                              packageClassType.getInternalName(),
                              null,
                              "java/lang/Object",
                              ArrayUtil.EMPTY_STRING_ARRAY
                );
                //We don't generate any source information for package with multiple files
                if (sourceFile != null) {
                    v.visitSource(sourceFile.getName(), null);
                }
                return v;
            }
        });
    }

    // Returns null if file has callables in several files
    @Nullable
    private JetFile getRepresentativePackageFile(@NotNull Collection<JetFile> packageFiles) {
        if (!previouslyCompiledCallables.isEmpty()) {
            return null;
        }

        List<JetFile> packageFilesWithCallables = PackagePartClassUtils.getPackageFilesWithCallables(packageFiles);
        return packageFilesWithCallables.size() == 1 ? packageFilesWithCallables.get(0) : null;
    }

    @Nullable
    private PackageFragmentDescriptor getCompiledPackageFragment(@NotNull FqName fqName) {
        if (!IncrementalCompilation.ENABLED) {
            return null;
        }

        // TODO rewrite it to something more robust when module system is implemented
        for (PackageFragmentDescriptor fragment : state.getModule().getPackageFragmentProvider().getPackageFragments(fqName)) {
            if (fragment instanceof IncrementalPackageFragmentProvider.IncrementalPackageFragment &&
                ((IncrementalPackageFragmentProvider.IncrementalPackageFragment) fragment).getModuleId().equals(state.getModuleId())) {
                return fragment;
            }
        }
        return null;
    }

    @NotNull
    private static List<DeserializedCallableMemberDescriptor> filterDeserializedCallables(@Nullable PackageFragmentDescriptor packageFragment) {
        if (packageFragment == null) {
            return Collections.emptyList();
        }
        List<DeserializedCallableMemberDescriptor> callables = Lists.newArrayList();
        for (DeclarationDescriptor member : packageFragment.getMemberScope().getAllDescriptors()) {
            if (member instanceof DeserializedCallableMemberDescriptor) {
                callables.add((DeserializedCallableMemberDescriptor) member);
            }
        }
        return callables;
    }

    private void generateDelegationsToPreviouslyCompiled(@NotNull Map<CallableMemberDescriptor, Runnable> generateCallableMemberTasks) {
        for (final DeserializedCallableMemberDescriptor member : previouslyCompiledCallables) {
            generateCallableMemberTasks.put(member, new Runnable() {
                @Override
                public void run() {
                    FieldOwnerContext context = CodegenContext.STATIC.intoPackageFacade(
                            AsmUtil.asmTypeByFqNameWithoutInnerClasses(PackagePartClassUtils.getPackagePartFqName(member)),
                            compiledPackageFragment
                    );

                    MemberCodegen<?> memberCodegen = createCodegenForPartOfPackageFacade(context);

                    if (member instanceof DeserializedSimpleFunctionDescriptor) {
                        DeserializedSimpleFunctionDescriptor function = (DeserializedSimpleFunctionDescriptor) member;
                        JvmMethodSignature signature = state.getTypeMapper().mapSignature(function, OwnerKind.PACKAGE);
                        memberCodegen.functionCodegen.generateMethod(OtherOrigin(function), signature, function,
                                                                     new FunctionGenerationStrategy() {
                                                                         @Override
                                                                         public void generateBody(
                                                                                 @NotNull MethodVisitor mv,
                                                                                 @NotNull JvmMethodSignature signature,
                                                                                 @NotNull MethodContext context,
                                                                                 @NotNull MemberCodegen<?> parentCodegen
                                                                         ) {
                                                                             throw new IllegalStateException("shouldn't be called");
                                                                         }
                                                                     }
                        );
                    }
                    else if (member instanceof DeserializedPropertyDescriptor) {
                        memberCodegen.propertyCodegen.generateInPackageFacade((DeserializedPropertyDescriptor) member);
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

        if (!generateCallableMemberTasks.isEmpty()) {
            generatePackageFacadeClass(generateCallableMemberTasks, bindings);
        }
    }

    private void generatePackageFacadeClass(
            @NotNull Map<CallableMemberDescriptor, Runnable> tasks,
            @NotNull List<JvmSerializationBindings> bindings
    ) {
        generateKotlinPackageReflectionField();

        generateDelegationsToPreviouslyCompiled(tasks);

        for (CallableMemberDescriptor member : Ordering.from(MemberComparator.INSTANCE).sortedCopy(tasks.keySet())) {
            tasks.get(member).run();
        }

        bindings.add(v.getSerializationBindings());
        writeKotlinPackageAnnotationIfNeeded(JvmSerializationBindings.union(bindings));
    }

    private void generateKotlinPackageReflectionField() {
        MethodVisitor mv = v.newMethod(NO_ORIGIN, ACC_STATIC, "<clinit>", "()V", null, null);
        Method method = method("kPackage", K_PACKAGE_IMPL_TYPE, getType(Class.class));
        InstructionAdapter iv = new InstructionAdapter(mv);
        MemberCodegen.generateReflectionObjectField(state, packageClassType, v, method, JvmAbi.KOTLIN_PACKAGE_FIELD_NAME, iv);
        iv.areturn(Type.VOID_TYPE);
        FunctionCodegen.endVisit(mv, "package facade static initializer", null);
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
        Collection<PackageFragmentDescriptor> packageFragments = Lists.newArrayList();
        ContainerUtil.addIfNotNull(packageFragments, packageFragment);
        ContainerUtil.addIfNotNull(packageFragments, compiledPackageFragment);
        ProtoBuf.Package packageProto = serializer.packageProto(packageFragments).build();

        if (packageProto.getMemberCount() == 0) return;

        PackageData data = new PackageData(createNameResolver(serializer.getNameTable()), packageProto);

        AnnotationVisitor av = v.newAnnotation(asmDescByFqNameWithoutInnerClasses(JvmAnnotationNames.KOTLIN_PACKAGE), true);
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
        boolean generatePackagePart = false;
        Type packagePartType = PackagePartClassUtils.getPackagePartType(file);
        PackageContext packagePartContext = CodegenContext.STATIC.intoPackagePart(packageFragment, packagePartType);

        for (JetDeclaration declaration : file.getDeclarations()) {
            if (declaration instanceof JetProperty || declaration instanceof JetNamedFunction) {
                generatePackagePart = true;
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

        if (!generatePackagePart) return null;

        ClassBuilder builder = state.getFactory().newVisitor(PackagePart(file, packageFragment), packagePartType, file);

        new PackagePartCodegen(builder, file, packagePartType, packagePartContext, state).generate();

        FieldOwnerContext packageFacade = CodegenContext.STATIC.intoPackageFacade(packagePartType, packageFragment);

        final MemberCodegen<?> memberCodegen = createCodegenForPartOfPackageFacade(packageFacade);

        for (final JetDeclaration declaration : file.getDeclarations()) {
            if (declaration instanceof JetNamedFunction || declaration instanceof JetProperty) {
                DeclarationDescriptor descriptor = state.getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration);
                assert descriptor instanceof CallableMemberDescriptor :
                        "Expected callable member, was " + descriptor + " for " + declaration.getText();
                generateCallableMemberTasks.put(
                        (CallableMemberDescriptor) descriptor,
                        new Runnable() {
                            @Override
                            public void run() {
                                memberCodegen.genFunctionOrProperty(declaration);
                            }
                        }
                );
            }
        }

        return builder;
    }

    private MemberCodegen<?> createCodegenForPartOfPackageFacade(@NotNull FieldOwnerContext packageFacade) {
        return new MemberCodegen<JetFile>(state, null, packageFacade, null, v) {
            @Override
            protected void generateDeclaration() {
                throw new UnsupportedOperationException();
            }

            @Override
            protected void generateBody() {
                throw new UnsupportedOperationException();
            }

            @Override
            protected void generateKotlinAnnotation() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Nullable
    private PackageFragmentDescriptor getOnlyPackageFragment(@NotNull FqName expectedFqName) {
        SmartList<PackageFragmentDescriptor> fragments = new SmartList<PackageFragmentDescriptor>();
        for (JetFile file : files) {
            PackageFragmentDescriptor fragment = state.getBindingContext().get(BindingContext.FILE_TO_PACKAGE_FRAGMENT, file);
            assert fragment != null : "package fragment is null for " + file;

            assert expectedFqName.equals(fragment.getFqName()) :
                    "expected package fq name: " + expectedFqName + ", actual: " + fragment.getFqName();

            if (!fragments.contains(fragment)) {
                fragments.add(fragment);
            }
        }
        if (fragments.size() > 1) {
            throw new IllegalStateException("More than one package fragment, files: " + files + " | fragments: " + fragments);
        }

        if (fragments.isEmpty()) {
            return null;
        }
        return fragments.get(0);
    }

    public void generateClassOrObject(@NotNull JetClassOrObject classOrObject) {
        JetFile file = classOrObject.getContainingJetFile();
        Type packagePartType = PackagePartClassUtils.getPackagePartType(file);
        CodegenContext context = CodegenContext.STATIC.intoPackagePart(packageFragment, packagePartType);
        MemberCodegen.genClassOrObject(context, classOrObject, state, null);
    }

    public void done() {
        v.done();
    }
}
