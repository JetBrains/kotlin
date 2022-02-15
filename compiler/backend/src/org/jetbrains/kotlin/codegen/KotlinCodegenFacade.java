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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus;
import org.jetbrains.kotlin.psi.KtFile;

import java.util.Collection;

public class KotlinCodegenFacade {
    public static void compileCorrectFiles(@NotNull GenerationState state, CodegenFactory codegenFactory) {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();

        state.beforeCompile();

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();

        CodegenFactory.IrConversionInput psi2irInput = CodegenFactory.IrConversionInput.Companion.fromGenerationState(state);
        CodegenFactory.BackendInput backendInput = codegenFactory.convertToIr(psi2irInput);

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();

        codegenFactory.generateModule(state, backendInput);

        CodegenFactory.Companion.doCheckCancelled(state);
        state.getFactory().done();
    }

    public static void generatePackage(@NotNull GenerationState state, @NotNull FqName packageFqName, @NotNull Collection<KtFile> files) {
        DefaultCodegenFactory.INSTANCE.generatePackage(state, packageFqName, files);
    }

    private KotlinCodegenFacade() {}
}
