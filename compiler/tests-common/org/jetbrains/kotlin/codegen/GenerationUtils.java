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

import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.cli.common.output.outputUtils.OutputUtilsKt;
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.descriptors.PackagePartProvider;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil;
import org.jetbrains.kotlin.test.KotlinTestUtils;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class GenerationUtils {
    private GenerationUtils() {
    }

    @NotNull
    public static ClassFileFactory compileFileTo(@NotNull KtFile ktFile, @NotNull KotlinCoreEnvironment environment, @NotNull File output) {
        ClassFileFactory factory = compileFile(ktFile, environment);
        OutputUtilsKt.writeAllTo(factory, output);
        return factory;
    }

    @NotNull
    public static ClassFileFactory compileFile(@NotNull KtFile ktFile, @NotNull KotlinCoreEnvironment environment) {
        return compileFiles(Collections.singletonList(ktFile), environment).getFactory();
    }

    @NotNull
    public static GenerationState compileFiles(@NotNull List<KtFile> files, @Nullable KotlinCoreEnvironment environment) {
        PackagePartProvider packagePartProvider =
                environment == null ? PackagePartProvider.Empty.INSTANCE : new JvmPackagePartProvider(environment);
        CompilerConfiguration configuration =
                environment == null ? KotlinTestUtils.newConfiguration() : environment.getConfiguration();

        AnalysisResult analysisResult =
                JvmResolveUtil.analyzeAndCheckForErrors(CollectionsKt.first(files).getProject(), files, configuration, packagePartProvider);
        analysisResult.throwIfError();

        GenerationState state = new GenerationState(
                CollectionsKt.first(files).getProject(), ClassBuilderFactories.TEST,
                analysisResult.getModuleDescriptor(), analysisResult.getBindingContext(),
                files, configuration
        );
        KotlinCodegenFacade.compileCorrectFiles(state, CompilationErrorHandler.THROW_EXCEPTION);
        return state;
    }
}
