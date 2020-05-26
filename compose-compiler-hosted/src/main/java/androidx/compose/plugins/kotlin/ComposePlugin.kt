/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin

import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.extensions.internal.CandidateInterceptor
import org.jetbrains.kotlin.extensions.internal.TypeResolutionInterceptor

class ComposeCommandLineProcessor : CommandLineProcessor {

    companion object {
        val PLUGIN_ID = "androidx.compose.plugins.kotlin"
    }

    override val pluginId =
        PLUGIN_ID
    override val pluginOptions = emptyList<CliOption>()

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration
    ) = throw CliOptionProcessingException("Unknown option: ${option.optionName}")
}

class ComposeComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(
        project: MockProject,
        configuration: CompilerConfiguration
    ) {
        registerProjectExtensions(
            project as Project,
            configuration
        )
    }

    companion object {

        @Suppress("UNUSED_PARAMETER")
        fun registerProjectExtensions(
            project: Project,
            configuration: CompilerConfiguration
        ) {
            StorageComponentContainerContributor.registerExtension(
                project,
                ComposableAnnotationChecker()
            )
            StorageComponentContainerContributor.registerExtension(
                project,
                UnionAnnotationCheckerProvider()
            )
            StorageComponentContainerContributor.registerExtension(
                project,
                TryCatchComposableChecker()
            )
            ComposeDiagnosticSuppressor.registerExtension(
                project,
                ComposeDiagnosticSuppressor()
            )
            TypeResolutionInterceptor.registerExtension(
                project,
                ComposeTypeResolutionInterceptorExtension()
            )
            IrGenerationExtension.registerExtension(project,
                ComposeIrGenerationExtension()
            )
            CandidateInterceptor.registerExtension(
                project,
                ComposeCallResolutionInterceptorExtension()
            )
        }
    }
}
