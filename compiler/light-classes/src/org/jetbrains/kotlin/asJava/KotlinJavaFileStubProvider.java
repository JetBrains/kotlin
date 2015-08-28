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

package org.jetbrains.kotlin.asJava;

import com.google.common.collect.Lists;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.ClassFileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.compiled.ClsFileImpl;
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub;
import com.intellij.psi.impl.java.stubs.impl.PsiJavaFileStubImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.PsiClassHolderFileStub;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.Stack;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.CompilationErrorHandler;
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade;
import org.jetbrains.kotlin.codegen.PackageCodegen;
import org.jetbrains.kotlin.codegen.binding.CodegenBinding;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.Progress;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.JetClassOrObject;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetPsiUtil;
import org.jetbrains.kotlin.psi.JetScript;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTraceContext;
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics;
import org.jetbrains.kotlin.resolve.jvm.JvmClassName;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.jetbrains.kotlin.resolve.DescriptorToSourceUtils.descriptorToDeclaration;

public class KotlinJavaFileStubProvider<T extends WithFileStubAndExtraDiagnostics> implements CachedValueProvider<T> {

    @NotNull
    public static KotlinJavaFileStubProvider<KotlinFacadeLightClassData> createForPackageClass(
            @NotNull final Project project,
            @NotNull final FqName packageFqName,
            @NotNull final GlobalSearchScope searchScope
    ) {
        return new KotlinJavaFileStubProvider<KotlinFacadeLightClassData>(
                project,
                false,
                new StubGenerationStrategy<KotlinFacadeLightClassData>() {
                    @NotNull
                    @Override
                    public LightClassConstructionContext getContext(@NotNull Collection<JetFile> files) {
                        return LightClassGenerationSupport.getInstance(project).getContextForPackage(files);
                    }

                    @NotNull
                    @Override
                    public Collection<JetFile> getFiles() {
                        // Don't memoize this, it can be called again after an out-of-code-block modification occurs,
                        // and the set of files changes
                        return LightClassGenerationSupport.getInstance(project).findFilesForPackage(packageFqName, searchScope);
                    }

                    @NotNull
                    @Override
                    public KotlinFacadeLightClassData createLightClassData(
                            PsiJavaFileStub javaFileStub,
                            BindingContext bindingContext,
                            Diagnostics extraDiagnostics
                    ) {
                        return new KotlinFacadeLightClassData(javaFileStub, extraDiagnostics);
                    }

                    @NotNull
                    @Override
                    public FqName getPackageFqName() {
                        return packageFqName;
                    }

                    @Override
                    public GenerationState.GenerateClassFilter getGenerateClassFilter() {
                        return new GenerationState.GenerateClassFilter() {

                            @Override
                            public boolean shouldGeneratePackagePart(JetFile jetFile) {
                                return true;
                            }

                            @Override
                            public boolean shouldAnnotateClass(JetClassOrObject classOrObject) {
                                return shouldGenerateClass(classOrObject);
                            }

                            @Override
                            public boolean shouldGenerateClass(JetClassOrObject classOrObject) {
                                // Top-level classes and such should not be generated for performance reasons.
                                // Local classes in top-level functions must still be generated
                                return JetPsiUtil.isLocal(classOrObject);
                            }

                            @Override
                            public boolean shouldGenerateScript(JetScript script) {
                                // Scripts yield top-level classes, and should not be generated
                                return false;
                            }
                        };
                    }

                    @Override
                    public void generate(@NotNull GenerationState state, @NotNull Collection<JetFile> files) {
                        PackageCodegen codegen = state.getFactory().forPackage(packageFqName, files);
                        codegen.generate(CompilationErrorHandler.THROW_EXCEPTION);
                        state.getFactory().asList();
                    }

                    @Override
                    public String toString() {
                        return StubGenerationStrategy.class.getName() + " for package class";
                    }
                }
        );
    }

    @NotNull
    public static CachedValueProvider<KotlinFacadeLightClassData> createForFacadeClass(
            @NotNull final Project project,
            @NotNull final FqName facadeFqName,
            @NotNull final GlobalSearchScope searchScope
    ) {
        return new KotlinJavaFileStubProvider<KotlinFacadeLightClassData>(
                project,
                false,
                new StubGenerationStrategy<KotlinFacadeLightClassData>() {
                    @NotNull
                    @Override
                    public Collection<JetFile> getFiles() {
                        return LightClassGenerationSupport.getInstance(project).findFilesForFacade(facadeFqName, searchScope);
                    }

                    @NotNull
                    @Override
                    public FqName getPackageFqName() {
                        return facadeFqName.parent();
                    }

                    @NotNull
                    @Override
                    public LightClassConstructionContext getContext(@NotNull Collection<JetFile> files) {
                        return LightClassGenerationSupport.getInstance(project).getContextForFacade(files);
                    }

                    @NotNull
                    @Override
                    public KotlinFacadeLightClassData createLightClassData(
                            PsiJavaFileStub javaFileStub,
                            BindingContext bindingContext,
                            Diagnostics extraDiagnostics
                    ) {
                        return new KotlinFacadeLightClassData(javaFileStub, extraDiagnostics);
                    }

                    @Override
                    public GenerationState.GenerateClassFilter getGenerateClassFilter() {
                        return new GenerationState.GenerateClassFilter() {
                            @Override
                            public boolean shouldAnnotateClass(JetClassOrObject classOrObject) {
                                return shouldGenerateClass(classOrObject);
                            }

                            @Override
                            public boolean shouldGenerateClass(JetClassOrObject classOrObject) {
                                return JetPsiUtil.isLocal(classOrObject);
                            }

                            @Override
                            public boolean shouldGeneratePackagePart(JetFile jetFile) {
                                return true;
                            }

                            @Override
                            public boolean shouldGenerateScript(JetScript script) {
                                return false;
                            }
                        };
                    }

                    @Override
                    public void generate(@NotNull GenerationState state, @NotNull Collection<JetFile> files) {
                        PackageCodegen codegen = state.getFactory().forFacade(facadeFqName, files);
                        codegen.generate(CompilationErrorHandler.THROW_EXCEPTION);
                        state.getFactory().asList();
                    }

                    @Override
                    public String toString() {
                        return StubGenerationStrategy.class.getName() + " for facade class";
                    }
                });
    }

    @NotNull
    public static KotlinJavaFileStubProvider<OutermostKotlinClassLightClassData> createForDeclaredClass(@NotNull final JetClassOrObject classOrObject) {
        return new KotlinJavaFileStubProvider<OutermostKotlinClassLightClassData>(
                classOrObject.getProject(),
                classOrObject.isLocal(),
                new StubGenerationStrategy<OutermostKotlinClassLightClassData>() {
                    private JetFile getFile() {
                        return classOrObject.getContainingJetFile();
                    }

                    @NotNull
                    @Override
                    public LightClassConstructionContext getContext(@NotNull Collection<JetFile> files) {
                        return LightClassGenerationSupport.getInstance(classOrObject.getProject()).getContextForClassOrObject(classOrObject);
                    }

                    @NotNull
                    @Override
                    public OutermostKotlinClassLightClassData createLightClassData(
                            PsiJavaFileStub javaFileStub,
                            BindingContext bindingContext,
                            Diagnostics extraDiagnostics
                    ) {
                        ClassDescriptor classDescriptor = bindingContext.get(BindingContext.CLASS, classOrObject);
                        if (classDescriptor == null) {
                            return new OutermostKotlinClassLightClassData(
                                    javaFileStub, extraDiagnostics, FqName.ROOT, classOrObject,
                                    Collections.<JetClassOrObject, InnerKotlinClassLightClassData>emptyMap()
                            );
                        }

                        FqName fqName = predictClassFqName(bindingContext, classDescriptor);
                        Collection<ClassDescriptor> allInnerClasses = CodegenBinding.getAllInnerClasses(bindingContext, classDescriptor);

                        Map<JetClassOrObject, InnerKotlinClassLightClassData> innerClassesMap = ContainerUtil.newHashMap();
                        for (ClassDescriptor innerClassDescriptor : allInnerClasses) {
                            PsiElement declaration = descriptorToDeclaration(innerClassDescriptor);
                            if (!(declaration instanceof JetClassOrObject)) continue;
                            JetClassOrObject innerClass = (JetClassOrObject) declaration;

                            InnerKotlinClassLightClassData innerLightClassData = new InnerKotlinClassLightClassData(
                                    predictClassFqName(bindingContext, innerClassDescriptor),
                                    innerClass
                            );

                            innerClassesMap.put(innerClass, innerLightClassData);
                        }

                        return new OutermostKotlinClassLightClassData(
                                javaFileStub,
                                extraDiagnostics,
                                fqName,
                                classOrObject,
                                innerClassesMap
                        );
                    }

                    @NotNull
                    private FqName predictClassFqName(BindingContext bindingContext, ClassDescriptor classDescriptor) {
                        Type asmType = CodegenBinding.getAsmType(bindingContext, classDescriptor);
                        //noinspection ConstantConditions
                        return JvmClassName.byInternalName(asmType.getClassName().replace('.', '/')).getFqNameForClassNameWithoutDollars();
                    }

                    @NotNull
                    @Override
                    public Collection<JetFile> getFiles() {
                        return Collections.singletonList(getFile());
                    }

                    @NotNull
                    @Override
                    public FqName getPackageFqName() {
                        return getFile().getPackageFqName();
                    }

                    @Override
                    public GenerationState.GenerateClassFilter getGenerateClassFilter() {
                        return new GenerationState.GenerateClassFilter() {

                            @Override
                            public boolean shouldGeneratePackagePart(JetFile jetFile) {
                                return true;
                            }

                            @Override
                            public boolean shouldAnnotateClass(JetClassOrObject classOrObject) {
                                return shouldGenerateClass(classOrObject);
                            }

                            @Override
                            public boolean shouldGenerateClass(JetClassOrObject generatedClassOrObject) {
                                // Trivial: generate and analyze class we are interested in.
                                if (generatedClassOrObject == classOrObject) return true;

                                // Process all parent classes as they are context for current class
                                // Process child classes because they probably affect members (heuristic)
                                if (PsiTreeUtil.isAncestor(generatedClassOrObject, classOrObject, true) ||
                                    PsiTreeUtil.isAncestor(classOrObject, generatedClassOrObject, true)) {
                                    return true;
                                }

                                if (generatedClassOrObject.isLocal() && classOrObject.isLocal()) {
                                    // Local classes should be process by CodegenAnnotatingVisitor to
                                    // decide what class they should be placed in.
                                    //
                                    // Example:
                                    // class A
                                    // fun foo() {
                                    //     trait Z: A {}
                                    //     fun bar() {
                                    //         class <caret>O2: Z {}
                                    //     }
                                    // }

                                    // TODO: current method will process local classes in irrelevant declarations, it should be fixed.
                                    PsiElement commonParent = PsiTreeUtil.findCommonParent(generatedClassOrObject, classOrObject);
                                    return commonParent != null && !(commonParent instanceof PsiFile);
                                }

                                return false;
                            }

                            @Override
                            public boolean shouldGenerateScript(JetScript script) {
                                // We generate all enclosing classes
                                return PsiTreeUtil.isAncestor(script, classOrObject, false);
                            }
                        };
                    }

                    @Override
                    public void generate(@NotNull GenerationState state, @NotNull Collection<JetFile> files) {
                        PackageCodegen packageCodegen = state.getFactory().forPackage(getPackageFqName(), files);
                        packageCodegen.generateClassOrObject(classOrObject);
                        state.getFactory().asList();
                    }

                    @Override
                    public String toString() {
                        return StubGenerationStrategy.class.getName() + " for explicit class " + classOrObject.getName();
                    }
                }
        );
    }

    private static final Logger LOG = Logger.getInstance(KotlinJavaFileStubProvider.class);

    private final Project project;
    private final StubGenerationStrategy<T> stubGenerationStrategy;
    private final boolean local;

    private KotlinJavaFileStubProvider(
            @NotNull Project project,
            boolean local,
            @NotNull StubGenerationStrategy<T> stubGenerationStrategy
    ) {
        this.project = project;
        this.stubGenerationStrategy = stubGenerationStrategy;
        this.local = local;
    }

    @Nullable
    @Override
    public Result<T> compute() {
        FqName packageFqName = stubGenerationStrategy.getPackageFqName();
        Collection<JetFile> files = stubGenerationStrategy.getFiles();

        checkForBuiltIns(packageFqName, files);

        LightClassConstructionContext context = stubGenerationStrategy.getContext(files);

        PsiJavaFileStub javaFileStub = createJavaFileStub(packageFqName, files);
        BindingContext bindingContext;
        BindingTraceContext forExtraDiagnostics = new BindingTraceContext();
        try {
            Stack<StubElement> stubStack = new Stack<StubElement>();
            stubStack.push(javaFileStub);

            GenerationState state = new GenerationState(
                    project,
                    new KotlinLightClassBuilderFactory(stubStack),
                    Progress.DEAF,
                    context.getModule(),
                    context.getBindingContext(),
                    Lists.newArrayList(files),
                    /*disable not-null assertions*/false, false,
                    /*generateClassFilter=*/stubGenerationStrategy.getGenerateClassFilter(),
                    /*disableInline=*/false,
                    /*disableOptimization=*/false,
                    null,
                    null,
                    forExtraDiagnostics,
                    null
            );
            KotlinCodegenFacade.prepareForCompilation(state);

            bindingContext = state.getBindingContext();

            stubGenerationStrategy.generate(state, files);

            StubElement pop = stubStack.pop();
            if (pop != javaFileStub) {
                LOG.error("Unbalanced stack operations: " + pop);
            }
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (RuntimeException e) {
            logErrorWithOSInfo(e, packageFqName, null);
            throw e;
        }

        Diagnostics extraDiagnostics = forExtraDiagnostics.getBindingContext().getDiagnostics();
        return Result.create(
                stubGenerationStrategy.createLightClassData(javaFileStub, bindingContext, extraDiagnostics),
                local ? PsiModificationTracker.MODIFICATION_COUNT : PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT
        );
    }

    @NotNull
    public static ClsFileImpl createFakeClsFile(
            @NotNull Project project,
            @NotNull final FqName packageFqName,
            @NotNull Collection<JetFile> files,
            @NotNull final Function0<? extends PsiClassHolderFileStub> fileStubProvider
    ) {
        PsiManager manager = PsiManager.getInstance(project);

        VirtualFile virtualFile = getRepresentativeVirtualFile(files);
        ClsFileImpl fakeFile = new ClsFileImpl(new ClassFileViewProvider(manager, virtualFile)) {
            @NotNull
            @Override
            public PsiClassHolderFileStub getStub() {
                return fileStubProvider.invoke();
            }

            @NotNull
            @Override
            public String getPackageName() {
                return packageFqName.asString();
            }
        };

        fakeFile.setPhysical(false);
        return fakeFile;
    }

    @NotNull
    private PsiJavaFileStub createJavaFileStub(@NotNull FqName packageFqName, @NotNull Collection<JetFile> files) {
        final PsiJavaFileStubImpl javaFileStub = new PsiJavaFileStubImpl(packageFqName.asString(), true);
        javaFileStub.setPsiFactory(new ClsWrapperStubPsiFactory());

        ClsFileImpl fakeFile = createFakeClsFile(project, packageFqName, files, new Function0<PsiClassHolderFileStub>() {
            @Override
            public PsiClassHolderFileStub invoke() {
                return javaFileStub;
            }
        });

        javaFileStub.setPsi(fakeFile);
        return javaFileStub;
    }

    @NotNull
    private static VirtualFile getRepresentativeVirtualFile(@NotNull Collection<JetFile> files) {
        JetFile firstFile = files.iterator().next();
        VirtualFile virtualFile = firstFile.getVirtualFile();
        assert virtualFile != null : "No virtual file for " + firstFile;
        return virtualFile;
    }

    private static void checkForBuiltIns(@NotNull FqName fqName, @NotNull Collection<JetFile> files) {
        for (JetFile file : files) {
            if (LightClassUtil.belongsToKotlinBuiltIns(file)) {
                // We may not fail later due to some luck, but generating JetLightClasses for built-ins is a bad idea anyways
                // If it fails later, there will be an exception logged
                logErrorWithOSInfo(null, fqName, file.getVirtualFile());
            }
        }
    }

    private static void logErrorWithOSInfo(@Nullable Throwable cause, @NotNull FqName fqName, @Nullable VirtualFile virtualFile) {
        String path = virtualFile == null ? "<null>" : virtualFile.getPath();
        LOG.error(
                "Could not generate LightClass for " + fqName + " declared in " + path + "\n" +
                "built-ins dir URL is " + LightClassUtil.getBuiltInsDirUrl() + "\n" +
                "System: " + SystemInfo.OS_NAME + " " + SystemInfo.OS_VERSION + " Java Runtime: " + SystemInfo.JAVA_RUNTIME_VERSION,
                cause);
    }

    private interface StubGenerationStrategy<T extends WithFileStubAndExtraDiagnostics> {
        @NotNull Collection<JetFile> getFiles();
        @NotNull FqName getPackageFqName();

        @NotNull LightClassConstructionContext getContext(@NotNull Collection<JetFile> files);
        @NotNull T createLightClassData(PsiJavaFileStub javaFileStub, BindingContext bindingContext, Diagnostics extraDiagnostics);

        GenerationState.GenerateClassFilter getGenerateClassFilter();
        void generate(@NotNull GenerationState state, @NotNull Collection<JetFile> files);
    }
}
