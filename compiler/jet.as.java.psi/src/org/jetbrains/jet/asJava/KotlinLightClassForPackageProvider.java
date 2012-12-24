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
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.psi.PsiClass;
import com.intellij.psi.impl.java.stubs.PsiClassStub;
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
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;
import java.util.List;

public class KotlinLightClassForPackageProvider implements CachedValueProvider<PsiClass> {
    private final Collection<JetFile> files;
    private final FqName fqName;
    private final Project project;

    public KotlinLightClassForPackageProvider(@NotNull Project project, @NotNull FqName fqName, @NotNull Collection<JetFile> files) {
        this.files = files;
        this.fqName = fqName;
        this.project = project;
    }

    @Nullable
    @Override
    public Result<PsiClass> compute() {
        checkForBuiltIns();

        PsiJavaFileStubImpl javaFileStub = new PsiJavaFileStubImpl(fqName.getFqName(), true);

        Stack<StubElement> stubStack = new Stack<StubElement>();

        ClassBuilderFactory builderFactory = new KotlinLightClassBuilderFactory(stubStack);

        // The context must reflect _all files in the module_. not only the current file
        // Otherwise, the analyzer gets confused and can't, for example, tell which files come as sources and which
        // must be loaded from .class files
        LightClassConstructionContext context = LightClassGenerationSupport.getInstance(project).analyzeRelevantCode(files);

        Throwable error = context.getError();
        if (error != null) {
            throw new IllegalStateException("failed to analyze: " + error, error);
        }

        try {
            GenerationState state = new GenerationState(
                    project,
                    builderFactory,
                    Progress.DEAF,
                    context.getBindingContext(),
                    Lists.newArrayList(files),
                    BuiltinToJavaTypesMapping.ENABLED,
                    /*not-null assertions*/false, false,
                    /*generateDeclaredClasses=*/false);

            GenerationStrategy strategy = new LightClassGenerationStrategy(new LightVirtualFile(), stubStack, javaFileStub);

            KotlinCodegenFacade.compileCorrectFiles(state, strategy, CompilationErrorHandler.THROW_EXCEPTION);
            state.getFactory().files();
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (RuntimeException e) {
            LightClassUtil.logErrorWithOSInfo(e, fqName, null);
            throw e;
        }

        FqName packageClassFqName = fqName.child(Name.identifier(JvmAbi.PACKAGE_CLASS));

        for (StubElement child : javaFileStub.getChildrenStubs()) {
            if (child instanceof PsiClassStub && Comparing.equal(packageClassFqName.getFqName(), ((PsiClassStub) child).getQualifiedName())) {
                PsiClass result = (PsiClass)child.getPsi();

                List<Object> dependencies = Lists.<Object>newArrayList(files);
                dependencies.add(PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT);
                return Result.create(result, files);
            }
        }

        throw new IllegalStateException("Namespace class was not found " + packageClassFqName + " for files " + files);
    }

    private void checkForBuiltIns() {
        for (JetFile file : files) {
            if (LightClassUtil.belongsToKotlinBuiltIns(file)) {
                // We may not fail later due to some luck, but generating JetLightClasses for built-ins is a bad idea anyways
                // If it fails later, there will be an exception logged
                LightClassUtil.logErrorWithOSInfo(null, fqName, file.getVirtualFile());
            }
        }
    }
}