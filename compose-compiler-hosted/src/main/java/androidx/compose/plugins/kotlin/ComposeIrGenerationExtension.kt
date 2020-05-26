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
import androidx.compose.plugins.kotlin.compiler.lower.ComposerIntrinsicTransformer
import androidx.compose.plugins.kotlin.compiler.lower.ComposerLambdaMemoization
import androidx.compose.plugins.kotlin.compiler.lower.ComposerParamTransformer
import androidx.compose.plugins.kotlin.compiler.lower.ComposableFunctionBodyTransformer
import androidx.compose.plugins.kotlin.compiler.lower.ComposeResolutionMetadataTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.getDeclaration
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace

class ComposeIrGenerationExtension : IrGenerationExtension {
    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        // TODO: refactor transformers to work with just BackendContext
        val bindingTrace = DelegatingBindingTrace(pluginContext.bindingContext, "trace in " +
                "ComposeIrGenerationExtension")

        // create a symbol remapper to be used across all transforms
        val symbolRemapper = DeepCopySymbolRemapper()

        // add metadata from the frontend onto IR Nodes so that the metadata will travel
        // with the ir nodes as they transform and get copied
        ComposeResolutionMetadataTransformer(pluginContext).lower(moduleFragment)

        // Memoize normal lambdas and wrap composable lambdas
        ComposerLambdaMemoization(pluginContext, symbolRemapper, bindingTrace).lower(moduleFragment)

        generateSymbols(pluginContext)

        // transform all composable functions to have an extra synthetic composer
        // parameter. this will also transform all types and calls to include the extra
        // parameter.
        ComposerParamTransformer(
            pluginContext,
            symbolRemapper,
            bindingTrace
        ).lower(moduleFragment)

        // transform calls to the currentComposer to just use the local parameter from the
        // previous transform
        ComposerIntrinsicTransformer(pluginContext).lower(moduleFragment)

        ComposableFunctionBodyTransformer(
            pluginContext,
            symbolRemapper,
            bindingTrace
        ).lower(moduleFragment)

        generateSymbols(pluginContext)

        // transform composable calls and emits into their corresponding calls appealing
        // to the composer
        ComposableCallTransformer(pluginContext, symbolRemapper, bindingTrace).lower(moduleFragment)

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
