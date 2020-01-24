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

import androidx.compose.plugins.kotlin.compiler.lower.ComposableCallTransformer
import androidx.compose.plugins.kotlin.compiler.lower.ComposeObservePatcher
import androidx.compose.plugins.kotlin.compiler.lower.ComposeResolutionMetadataTransformer
import androidx.compose.plugins.kotlin.compiler.lower.ComposerIntrinsicTransformer
import androidx.compose.plugins.kotlin.compiler.lower.ComposerParamTransformer
import androidx.compose.plugins.kotlin.frames.FrameIrTransformer
import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.resolve.BindingContext

class ComposeIrGenerationExtension : IrGenerationExtension {
    override fun generate(
        file: IrFile,
        backendContext: BackendContext,
        bindingContext: BindingContext
    ) {
        // TODO: refactor transformers to work with just BackendContext
        val jvmContext = backendContext as JvmBackendContext
        val module = jvmContext.ir.irModule
        if (ComposeFlags.COMPOSER_PARAM) {
            // With the COMPOSER_PARAM flag, we transform the entire module all at once since
            // we end up remapping symbols, we need to ensure that everything in the module
            // points to the right symbol. There is no extension point that allows you to transform
            // at the module level but we should communicate this need with JetBrains as it seems
            // like the only reasonable way to update top-level symbols. If a module-based
            // extension point gets added, we should refactor this to use it.
            if (file == module.files.first()) {

                // create a symbol remapper to be used across all transforms
                val symbolRemapper = DeepCopySymbolRemapper()

                // add metadata from the frontend onto IR Nodes so that the metadata will travel
                // with the ir nodes as they transform and get copied
                ComposeResolutionMetadataTransformer(jvmContext).lower(module)

                // transform @Model classes
                FrameIrTransformer(jvmContext).lower(module)

                // transform all composable functions to have an extra synthetic composer
                // parameter. this will also transform all types and calls to include the extra
                // parameter.
                ComposerParamTransformer(jvmContext, symbolRemapper).lower(module)

                // transform calls to the currentComposerIntrinsic to just use the local
                // parameter from the previous transform
                ComposerIntrinsicTransformer(jvmContext).lower(module)

                // transform composable calls and emits into their corresponding calls appealing
                // to the composer
                ComposableCallTransformer(jvmContext, symbolRemapper).lower(module)

                // transform composable functions to have restart groups so that they can be
                // recomposed
                ComposeObservePatcher(jvmContext, symbolRemapper).lower(module)
            }
            return
        }

        FrameIrTransformer(jvmContext).lower(file)
        ComposableCallTransformer(jvmContext, DeepCopySymbolRemapper()).lower(file)
        ComposeObservePatcher(jvmContext, DeepCopySymbolRemapper()).lower(file)
    }
}
