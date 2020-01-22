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

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import androidx.compose.plugins.kotlin.compiler.lower.ComposableCallTransformer
import androidx.compose.plugins.kotlin.compiler.lower.ComposeObservePatcher
import androidx.compose.plugins.kotlin.compiler.lower.ComposeSymbolPatcherTransformer
import androidx.compose.plugins.kotlin.compiler.lower.ComposerIntrinsicTransformer
import androidx.compose.plugins.kotlin.compiler.lower.ComposerParamTransformer
import androidx.compose.plugins.kotlin.frames.FrameIrTransformer
import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.resolve.BindingContext

class ComposeIrGenerationExtension : IrGenerationExtension {
    override fun generate(
        file: IrFile,
        backendContext: BackendContext,
        bindingContext: BindingContext
    ) {
        // TODO: refactor transformers to work with just BackendContext
        val jvmContext = backendContext as JvmBackendContext
        if (ComposeFlags.COMPOSER_PARAM) {
            FrameIrTransformer(jvmContext).lower(file)
            ComposerParamTransformer(jvmContext).lower(file)
            ComposerIntrinsicTransformer(jvmContext).lower(file)
            ComposableCallTransformer(jvmContext).lower(file)
            ComposeObservePatcher(jvmContext).lower(file)
            ComposeSymbolPatcherTransformer(jvmContext).lower(file)
            return
        }

        FrameIrTransformer(jvmContext).lower(file)
        ComposableCallTransformer(jvmContext).lower(file)
        ComposeObservePatcher(jvmContext).lower(file)
    }
}
