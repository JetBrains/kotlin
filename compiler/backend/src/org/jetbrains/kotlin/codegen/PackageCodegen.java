/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import kotlin.Function0;
import org.jetbrains.annotations.Mutable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.context.CodegenContext;
import org.jetbrains.kotlin.codegen.context.FieldOwnerContext;
import org.jetbrains.kotlin.codegen.context.MethodContext;
import org.jetbrains.kotlin.codegen.context.PackageContext;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.config.IncrementalCompilation;
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor;
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.load.java.JvmAnnotationNames;
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils;
import org.jetbrains.kotlin.load.kotlin.incremental.IncrementalPackageFragmentProvider;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.MemberComparator;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature;
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.serialization.DescriptorSerializer;
import org.jetbrains.kotlin.serialization.PackageData;
import org.jetbrains.kotlin.serialization.ProtoBuf;
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor;
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor;
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor;
import org.jetbrains.kotlin.serialization.jvm.BitEncoding;
import org.jetbrains.org.objectweb.asm.AnnotationVisitor;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.util.*;

import static org.jetbrains.kotlin.codegen.AsmUtil.asmDescByFqNameWithoutInnerClasses;
import static org.jetbrains.kotlin.codegen.AsmUtil.method;
import static org.jetbrains.kotlin.load.kotlin.PackageClassUtils.getPackageClassFqName;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.K_PACKAGE_IMPL_TYPE;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.getType;
import static org.jetbrains.kotlin.resolve.jvm.diagnostics.DiagnosticsPackage.*;
import static org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin.NO_ORIGIN;
import static org.jetbrains.kotlin.serialization.NameSerializationUtil.createNameResolver;
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
        for (DeclarationDescriptor member : packageFragment.getMemberScope().getDescriptors(DescriptorKindFilter.CALLABLES, JetScope.ALL_NAME_FILTER)) {
            if (member instanceof DeserializedCallableMemberDescriptor) {
                callables.add((DeserializedCallableMemberDescriptor) member);
            }
        }
        return callables;
    }

    private void generateDelegationsToPreviouslyCompiled(@NotNull @Mutable Map<CallableMemberDescriptor, Runnable> generateCallableMemberTasks) {
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
                                                                                 @NotNull FrameMap frameMap,
                                                                                 @NotNull JvmMethodSignature signature,
                                                                                 @NotNull MethodContext context,
                                                                                 @NotNull MemberCodegen<?> parentCodegen
                                                                         ) {
                                                                             throw new IllegalStateException("shouldn't be called");
                                                                         }
                                                                     }
                        );

                        memberCodegen.functionCodegen.generateDefaultIfNeeded(
                                context.intoFunction(function), signature, function, OwnerKind.PACKAGE,
                                DefaultParameterValueLoader.DEFAULT, null);

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

        generateDelegationsToPreviouslyCompiled(generateCallableMemberTasks);

        if (!generateCallableMemberTasks.isEmpty()) {
            generatePackageFacadeClass(generateCallableMemberTasks, bindings);
        }
    }

    private void generatePackageFacadeClass(
            @NotNull Map<CallableMemberDescriptor, Runnable> tasks,
            @NotNull List<JvmSerializationBindings> bindings
    ) {
        generateKotlinPackageReflectionField();

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

        DescriptorSerializer serializer = DescriptorSerializer.createTopLevel(new JvmSerializerExtension(bindings));
        Collection<PackageFragmentDescriptor> packageFragments = Lists.newArrayList();
        ContainerUtil.addIfNotNull(packageFragments, packageFragment);
        ContainerUtil.addIfNotNull(packageFragments, compiledPackageFragment);
        ProtoBuf.Package packageProto = serializer.packageProto(packageFragments).build();

        if (packageProto.getMemberCount() == 0) return;

        PackageData data = new PackageData(createNameResolver(serializer.getStringTable()), packageProto);

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
                if (state.getGenerateDeclaredClassFilter().shouldProcessClass(classOrObject)) {
                    generateClassOrObject(classOrObject);
                }
            }
            else if (declaration instanceof JetScript) {
                JetScript script = (JetScript) declaration;

               // SCRIPT: generate script code, should be separate execution branch
                if (state.getGenerateDeclaredClassFilter().shouldProcessScript(script)) {
                    ScriptCodegen.createScriptCodegen(script, state, packagePartContext).generate();
                }
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
            assert fragment != null : "package fragment is null for " + file + "\n" + file.getText();

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
