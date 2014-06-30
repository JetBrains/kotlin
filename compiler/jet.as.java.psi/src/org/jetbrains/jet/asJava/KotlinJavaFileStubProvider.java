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

package org.jetbrains.jet.asJava;

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
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.psi.impl.compiled.ClsFileImpl;
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub;
import com.intellij.psi.impl.java.stubs.impl.PsiJavaFileStubImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.PsiClassHolderFileStub;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.CompilationErrorHandler;
import org.jetbrains.jet.codegen.KotlinCodegenFacade;
import org.jetbrains.jet.codegen.PackageCodegen;
import org.jetbrains.jet.codegen.binding.CodegenBinding;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.Progress;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.Diagnostics;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class KotlinJavaFileStubProvider<T extends WithFileStubAndExtraDiagnostics> implements CachedValueProvider<T> {

    @NotNull
    public static KotlinJavaFileStubProvider<KotlinPackageLightClassData> createForPackageClass(
            @NotNull final Project project,
            @NotNull final FqName packageFqName,
            @NotNull final GlobalSearchScope searchScope
    ) {
        return new KotlinJavaFileStubProvider<KotlinPackageLightClassData>(
                project,
                false,
                new StubGenerationStrategy<KotlinPackageLightClassData>() {
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
                    public KotlinPackageLightClassData createLightClassData(
                            PsiJavaFileStub javaFileStub,
                            BindingContext bindingContext,
                            Diagnostics extraDiagnostics
                    ) {
                        return new KotlinPackageLightClassData(javaFileStub, extraDiagnostics);
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
                            public boolean shouldProcess(JetClassOrObject classOrObject) {
                                // Top-level classes and such should not be generated for performance reasons.
                                // Local classes in top-level functions must still be generated
                                return JetPsiUtil.isLocal(classOrObject);
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
                                    javaFileStub, extraDiagnostics,
                                    "", classOrObject, null, Collections.<JetClassOrObject, InnerKotlinClassLightClassData>emptyMap()
                            );
                        }

                        String jvmInternalName = CodegenBinding.getJvmInternalName(bindingContext, classDescriptor);
                        Collection<ClassDescriptor> allInnerClasses = CodegenBinding.getAllInnerClasses(bindingContext, classDescriptor);

                        Map<JetClassOrObject, InnerKotlinClassLightClassData> innerClassesMap = ContainerUtil.newHashMap();
                        for (ClassDescriptor innerClassDescriptor : allInnerClasses) {
                            JetClassOrObject innerClass = (JetClassOrObject) BindingContextUtils.descriptorToDeclaration(
                                    bindingContext, innerClassDescriptor
                            );
                            if (innerClass == null) continue;

                            InnerKotlinClassLightClassData innerLightClassData = new InnerKotlinClassLightClassData(
                                    CodegenBinding.getJvmInternalName(bindingContext, innerClassDescriptor),
                                    innerClass,
                                    innerClassDescriptor
                            );

                            innerClassesMap.put(innerClass, innerLightClassData);
                        }

                        return new OutermostKotlinClassLightClassData(
                                javaFileStub,
                                extraDiagnostics,
                                jvmInternalName,
                                classOrObject,
                                classDescriptor,
                                innerClassesMap
                        );
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
                            public boolean shouldProcess(JetClassOrObject generatedClassOrObject) {
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

        PsiJavaFileStub javaFileStub = createJavaFileStub(packageFqName, getRepresentativeVirtualFile(files));
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
                    /*not-null assertions*/false, false,
                    /*generateClassFilter=*/stubGenerationStrategy.getGenerateClassFilter(),
                    /*to generate inline flag on methods*/true,
                    null,
                    null,
                    forExtraDiagnostics,
                    null);
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
    private PsiJavaFileStub createJavaFileStub(@NotNull final FqName packageFqName, @NotNull VirtualFile virtualFile) {
        PsiManager manager = PsiManager.getInstance(project);

        final PsiJavaFileStubImpl javaFileStub = new PsiJavaFileStubImpl(packageFqName.asString(), true);
        javaFileStub.setPsiFactory(new ClsWrapperStubPsiFactory());

        ClsFileImpl fakeFile =
                new ClsFileImpl((PsiManagerImpl) manager, new ClassFileViewProvider(manager, virtualFile)) {
                    @NotNull
                    @Override
                    public PsiClassHolderFileStub getStub() {
                        return javaFileStub;
                    }

                    @NotNull
                    @Override
                    public String getPackageName() {
                        return packageFqName.asString();
                    }
                };

        fakeFile.setPhysical(false);
        javaFileStub.setPsi(fakeFile);
        return javaFileStub;
    }

    @NotNull
    private static VirtualFile getRepresentativeVirtualFile(@NotNull Collection<JetFile> files) {
        JetFile firstFile = files.iterator().next();
        VirtualFile virtualFile = files.size() == 1 ? firstFile.getVirtualFile() : new LightVirtualFile();
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