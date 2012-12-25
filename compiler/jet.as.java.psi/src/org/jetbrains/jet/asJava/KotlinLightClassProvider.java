/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub;
import com.intellij.psi.impl.java.stubs.impl.PsiJavaFileStubImpl;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.*;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.GenerationStrategy;
import org.jetbrains.jet.codegen.state.Progress;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Collection;
import java.util.Collections;

public class KotlinLightClassProvider implements CachedValueProvider<PsiJavaFileStub> {

    @NotNull
    public static KotlinLightClassProvider createForPackageClass(
            @NotNull Project project,
            @NotNull FqName packageFqName,
            @NotNull Collection<JetFile> files
    ) {
        return new KotlinLightClassProvider(project, packageFqName, files, new StubGenerationStrategy.NoDeclaredClasses() {
            @Override
            public void generate(GenerationState state, GenerationStrategy strategy) {
                KotlinCodegenFacade.compileCorrectFiles(state, strategy, CompilationErrorHandler.THROW_EXCEPTION);

                state.getFactory().files();
            }
        });
    }

    @NotNull
    public static KotlinLightClassProvider createForDeclaredTopLevelClass(
            @NotNull final JetClassOrObject classOrObject
    ) {
        JetFile file = (JetFile) classOrObject.getContainingFile();
        assert classOrObject.getParent() == file : "Not a top-level class: " + classOrObject.getText();

        final FqName packageFqName = JetPsiUtil.getFQName(file);

        return new KotlinLightClassProvider(classOrObject.getProject(), packageFqName, Collections.singletonList(file), new StubGenerationStrategy.WithDeclaredClasses() {
            @Override
            public void generate(GenerationState state, GenerationStrategy strategy) {

                NamespaceCodegen namespaceCodegen = state.getFactory().forNamespace(packageFqName, state.getFiles());
                NamespaceDescriptor namespaceDescriptor =
                        state.getBindingContext().get(BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR, packageFqName);
                assert namespaceDescriptor != null : "No package descriptor for " + packageFqName + " for class " + classOrObject.getText();
                namespaceCodegen.generateClassOrObject(namespaceDescriptor, classOrObject);

                state.getFactory().files();
            }
        });
    }

    private static final Logger LOG = Logger.getInstance(KotlinLightClassProvider.class);

    private final Collection<JetFile> files;
    private final FqName packageFqName;
    private final Project project;
    private final StubGenerationStrategy stubGenerationStrategy;

    private KotlinLightClassProvider(
            @NotNull Project project,
            @NotNull FqName packageFqName,
            @NotNull Collection<JetFile> files,
            @NotNull StubGenerationStrategy stubGenerationStrategy
    ) {
        this.files = files;
        this.packageFqName = packageFqName;
        this.project = project;
        this.stubGenerationStrategy = stubGenerationStrategy;
    }

    @Nullable
    @Override
    public Result<PsiJavaFileStub> compute() {
        checkForBuiltIns(packageFqName, files);

        LightClassConstructionContext context = LightClassGenerationSupport.getInstance(project).analyzeRelevantCode(files);

        Throwable error = context.getError();
        if (error != null) {
            throw new IllegalStateException("failed to analyze: " + error, error);
        }

        PsiJavaFileStubImpl javaFileStub = new PsiJavaFileStubImpl(packageFqName.getFqName(), true);

        try {
            Stack<StubElement> stubStack = new Stack<StubElement>();
            ClassBuilderFactory builderFactory = new KotlinLightClassBuilderFactory(stubStack);
            GenerationState state = new GenerationState(
                    project,
                    builderFactory,
                    Progress.DEAF,
                    context.getBindingContext(),
                    Lists.newArrayList(files),
                    BuiltinToJavaTypesMapping.ENABLED,
                    /*not-null assertions*/false, false,
                    /*generateDeclaredClasses=*/stubGenerationStrategy.generateDeclaredClasses());
            GenerationStrategy strategy = new LightClassGenerationStrategy(new LightVirtualFile(), stubStack, javaFileStub);

            stubGenerationStrategy.generate(state, strategy);
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (RuntimeException e) {
            logErrorWithOSInfo(e, packageFqName, null);
            throw e;
        }

        return Result.<PsiJavaFileStub>create(javaFileStub, PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT);
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
        boolean generateDeclaredClasses();
        void generate(GenerationState state, GenerationStrategy strategy);

        abstract class NoDeclaredClasses implements StubGenerationStrategy {
            @Override
            public boolean generateDeclaredClasses() {
                return false;
            }
        }

        abstract class WithDeclaredClasses implements StubGenerationStrategy {
            @Override
            public boolean generateDeclaredClasses() {
                return true;
            }
        }
    }
}