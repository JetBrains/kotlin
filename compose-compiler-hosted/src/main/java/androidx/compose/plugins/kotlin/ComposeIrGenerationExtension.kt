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
import androidx.compose.plugins.kotlin.compiler.lower.ComposerIntrinsicTransformer
import androidx.compose.plugins.kotlin.compiler.lower.ComposerLambdaMemoization
import androidx.compose.plugins.kotlin.compiler.lower.ComposerParamTransformer
import androidx.compose.plugins.kotlin.compiler.lower.ComposableFunctionBodyTransformer
import androidx.compose.plugins.kotlin.compiler.lower.ComposeResolutionMetadataTransformer
import androidx.compose.plugins.kotlin.frames.FrameIrTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.getDeclaration
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace

object ComposeTransforms {
    const val DEFAULT = 0b00111111
    const val NONE = 0b00000000
    const val FRAMED_CLASSES = 0b00000001
    const val LAMBDA_MEMOIZATION = 0b00000010
    const val COMPOSER_PARAM = 0b00000100
    const val INTRINSICS = 0b00001000
    const val CALLS_AND_EMITS = 0b00010000
    const val RESTART_GROUPS = 0b00100000
    const val CONTROL_FLOW_GROUPS = 0b01000000
    const val FUNCTION_BODY_SKIPPING = 0b10000000
}

class ComposeIrGenerationExtension : IrGenerationExtension {
    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) = generate(
        moduleFragment,
        pluginContext,
        transforms = ComposeTransforms.DEFAULT
    )

    fun generate(
        module: IrModuleFragment,
        pluginContext: IrPluginContext,
        transforms: Int
    ) {
        // TODO: refactor transformers to work with just BackendContext
        val bindingTrace = DelegatingBindingTrace(pluginContext.bindingContext, "trace in " +
                "ComposeIrGenerationExtension")

        // create a symbol remapper to be used across all transforms
        val symbolRemapper = DeepCopySymbolRemapper()

        // add metadata from the frontend onto IR Nodes so that the metadata will travel
        // with the ir nodes as they transform and get copied
        ComposeResolutionMetadataTransformer(pluginContext).lower(module)

            // transform @Model classes
            if (transforms and ComposeTransforms.FRAMED_CLASSES != 0) {
                FrameIrTransformer(pluginContext).lower(module)
            }

            // Memoize normal lambdas and wrap composable lambdas
            if (transforms and ComposeTransforms.LAMBDA_MEMOIZATION != 0) {
                ComposerLambdaMemoization(pluginContext, symbolRemapper, bindingTrace).lower(module)
            }

            val functionBodySkipping = transforms and ComposeTransforms.FUNCTION_BODY_SKIPPING != 0

        generateSymbols(pluginContext)

            // transform all composable functions to have an extra synthetic composer
            // parameter. this will also transform all types and calls to include the extra
            // parameter.
            if (transforms and ComposeTransforms.COMPOSER_PARAM != 0) {
                ComposerParamTransformer(
                    pluginContext,
                    symbolRemapper,
                    bindingTrace,
                    functionBodySkipping
                ).lower(module)
            } else if (functionBodySkipping) {
                error("Cannot have FUNCTION_BODY_SKIPPING on without COMPOSER_PARAM")
            }

            // transform calls to the currentComposer to just use the local parameter from the
            // previous transform
            if (transforms and ComposeTransforms.INTRINSICS != 0) {
                ComposerIntrinsicTransformer(pluginContext, functionBodySkipping).lower(module)
            }

            if (transforms and ComposeTransforms.CONTROL_FLOW_GROUPS != 0) {
                ComposableFunctionBodyTransformer(
                    pluginContext,
                    symbolRemapper,
                    bindingTrace
                ).lower(module)
            }

            generateSymbols(pluginContext)

            // transform composable calls and emits into their corresponding calls appealing
            // to the composer
            if (transforms and ComposeTransforms.CALLS_AND_EMITS != 0) {
                ComposableCallTransformer(pluginContext, symbolRemapper, bindingTrace).lower(module)
            }

            generateSymbols(pluginContext)

            // transform composable functions to have restart groups so that they can be
            // recomposed
            if (transforms and ComposeTransforms.RESTART_GROUPS != 0) {
                ComposeObservePatcher(pluginContext, symbolRemapper, bindingTrace).lower(module)
            }
            generateSymbols(pluginContext)
    }
}

val SymbolTable.allUnbound: List<IrSymbol>
    get() {
        val r = mutableListOf<IrSymbol>()
        r.addAll(unboundClasses)
        r.addAll(unboundConstructors)
        r.addAll(unboundEnumEntries)
        r.addAll(unboundFields)
        r.addAll(unboundSimpleFunctions)
        r.addAll(unboundProperties)
        r.addAll(unboundTypeParameters)
        r.addAll(unboundTypeAliases)
        return r
    }

fun generateSymbols(pluginContext: IrPluginContext) {
    lateinit var unbound: List<IrSymbol>
    val visited = mutableSetOf<IrSymbol>()
    do {
        unbound = pluginContext.symbolTable.allUnbound

        for (symbol in unbound) {
            if (visited.contains(symbol)) {
                continue
            }
            // Symbol could get bound as a side effect of deserializing other symbols.
            if (!symbol.isBound) {
                pluginContext.irProviders.getDeclaration(symbol)
            }
            if (!symbol.isBound) { visited.add(symbol) }
        }
    } while ((unbound - visited).isNotEmpty())
}
