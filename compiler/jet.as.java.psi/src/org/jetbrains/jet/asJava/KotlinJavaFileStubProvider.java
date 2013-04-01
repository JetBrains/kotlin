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

package org.jetbrains.jet.asJava;

import com.google.common.collect.Lists;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.ClassFileViewProvider;
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
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.BuiltinToJavaTypesMapping;
import org.jetbrains.jet.codegen.CompilationErrorHandler;
import org.jetbrains.jet.codegen.NamespaceCodegen;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.Progress;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Collection;
import java.util.Collections;

public class KotlinJavaFileStubProvider implements CachedValueProvider<PsiJavaFileStub> {

    @NotNull
    public static KotlinJavaFileStubProvider createForPackageClass(
            @NotNull final Project project,
            @NotNull final FqName packageFqName,
            @NotNull final GlobalSearchScope searchScope
    ) {
        return new KotlinJavaFileStubProvider(project, new StubGenerationStrategy.NoDeclaredClasses() {

            @NotNull
            @Override
            public Collection<JetFile> getFiles() {
                // Don't memoize this, it can be called again after an out-of-code-block modification occurs,
                // and the set of files changes
                return LightClassGenerationSupport.getInstance(project).findFilesForPackage(packageFqName, searchScope);
            }

            @NotNull
            @Override
            public FqName getPackageFqName() {
                return packageFqName;
            }

            @Override
            public void generate(@NotNull GenerationState state, @NotNull Collection<JetFile> files) {
                NamespaceCodegen codegen = state.getFactory().forNamespace(packageFqName, files);
                codegen.generate(CompilationErrorHandler.THROW_EXCEPTION);
                state.getFactory().files();
            }
        });
    }

    @NotNull
    public static KotlinJavaFileStubProvider createForDeclaredTopLevelClass(
            @NotNull final JetClassOrObject classOrObject
    ) {
        return new KotlinJavaFileStubProvider(classOrObject.getProject(), new StubGenerationStrategy.WithDeclaredClasses() {
            private JetFile getFile() {
                JetFile file = (JetFile) classOrObject.getContainingFile();
                assert classOrObject.getParent() == file : "Not a top-level class: " + classOrObject.getText();
                return file;
            }

            @NotNull
            @Override
            public Collection<JetFile> getFiles() {
                return Collections.singletonList(getFile());
            }

            @NotNull
            @Override
            public FqName getPackageFqName() {
                return JetPsiUtil.getFQName(getFile());
            }

            @Override
            public void generate(@NotNull GenerationState state, @NotNull Collection<JetFile> files) {
                FqName packageFqName = getPackageFqName();
                NamespaceCodegen namespaceCodegen = state.getFactory().forNamespace(packageFqName, files);
                NamespaceDescriptor namespaceDescriptor =
                        state.getBindingContext().get(BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR, packageFqName);
                assert namespaceDescriptor != null : "No package descriptor for " + packageFqName + " for class " + classOrObject.getText();
                namespaceCodegen.generateClassOrObject(namespaceDescriptor, classOrObject);

                state.getFactory().files();
            }
        });
    }

    private static final Logger LOG = Logger.getInstance(KotlinJavaFileStubProvider.class);

    private final Project project;
    private final StubGenerationStrategy stubGenerationStrategy;

    private KotlinJavaFileStubProvider(
            @NotNull Project project,
            @NotNull StubGenerationStrategy stubGenerationStrategy
    ) {
        this.project = project;
        this.stubGenerationStrategy = stubGenerationStrategy;
    }

    @Nullable
    @Override
    public Result<PsiJavaFileStub> compute() {
        FqName packageFqName = stubGenerationStrategy.getPackageFqName();
        Collection<JetFile> files = stubGenerationStrategy.getFiles();

        checkForBuiltIns(packageFqName, files);

        LightClassConstructionContext context = LightClassGenerationSupport.getInstance(project).analyzeRelevantCode(files);

        Throwable error = context.getError();
        if (error != null) {
            throw new IllegalStateException("failed to analyze: " + error, error);
        }

        PsiJavaFileStub javaFileStub = createJavaFileStub(packageFqName, getRepresentativeVirtualFile(files));
        try {
            Stack<StubElement> stubStack = new Stack<StubElement>();
            stubStack.push(javaFileStub);

            GenerationState state = new GenerationState(
                    project,
                    new KotlinLightClassBuilderFactory(stubStack),
                    Progress.DEAF,
                    context.getBindingContext(),
                    Lists.newArrayList(files),
                    BuiltinToJavaTypesMapping.ENABLED,
                    /*not-null assertions*/false, false,
                    /*generateDeclaredClasses=*/stubGenerationStrategy.generateDeclaredClasses());
            state.beforeCompile();

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

        return Result.create(javaFileStub, PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT);
    }

    @NotNull
    private PsiJavaFileStub createJavaFileStub(@NotNull final FqName packageFqName, @NotNull VirtualFile virtualFile) {
        PsiManager manager = PsiManager.getInstance(project);

        final PsiJavaFileStubImpl javaFileStub = new PsiJavaFileStubImpl(packageFqName.getFqName(), true);
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
                        return packageFqName.getFqName();
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
                "built-ins dir URL is " + LightClassUtil.getBuiltInsDirResourceUrl() + "\n" +
                "System: " + SystemInfo.OS_NAME + " " + SystemInfo.OS_VERSION + " Java Runtime: " + SystemInfo.JAVA_RUNTIME_VERSION,
                cause);
    }

    private interface StubGenerationStrategy {
        @NotNull Collection<JetFile> getFiles();
        @NotNull FqName getPackageFqName();
        boolean generateDeclaredClasses();
        void generate(@NotNull GenerationState state, @NotNull Collection<JetFile> files);

        abstract class NoDeclaredClasses implements StubGenerationStrategy {
            @Override
            public boolean generateDeclaredClasses() {
                return false;
            }

            @Override
            public String toString() {
                // For subclasses to be identifiable in the debugger
                return NoDeclaredClasses.class.getSimpleName();
            }
        }

        abstract class WithDeclaredClasses implements StubGenerationStrategy {
            @Override
            public boolean generateDeclaredClasses() {
                return true;
            }

            @Override
            public String toString() {
                // For subclasses to be identifiable in the debugger
                return WithDeclaredClasses.class.getSimpleName();
            }
        }
    }
}