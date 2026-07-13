/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.dependencies.logic

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

private var IrValueParameter.closestOverriddenDefaultParameter: IrValueParameterSymbol? by irAttribute(copyByDefault = false)

val IrValueParameterSymbol.closestOverriddenDefaultParameter: IrValueParameterSymbol? get() = owner.closestOverriddenDefaultParameter

class PathCompressingFinder<T>(val parents: (T) -> Sequence<T>) {
    private val parentMap = mutableMapOf<T, T>()
    fun find(element: T): T {
        if (parentMap[element] == element) return element
        parents(element).forEach {
            // All parents must have exactly ONE root ancestor
            parentMap[element] = find(it)
        }
        return parentMap[element] ?: element
    }
}

object DefaultFunctionParametersCollector : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.files.forEach { file ->

            file.acceptChildrenVoid(object : IrVisitorVoid() {

                private val visitedFunctions = mutableSetOf<IrFunctionSymbol>()

                private val rootOverriddenFunctionFinder = PathCompressingFinder<IrSimpleFunctionSymbol> {
                    it.owner.overriddenSymbols.asSequence()
                }

                private val IrSimpleFunction.rootOverriddenFunction: IrSimpleFunctionSymbol
                    get() = rootOverriddenFunctionFinder.find(symbol)

                private fun IrSimpleFunctionSymbol.propagateDefaultParameters(parameterMap: MutableMap<Int, IrValueParameterSymbol> = mutableMapOf()) {
                    if (!visitedFunctions.add(this)) return
                    owner.parameters.forEachIndexed { index, parameter ->
                        if (parameter.defaultValue != null) parameterMap[index] = parameter.symbol
                        if (index in parameterMap) parameter.closestOverriddenDefaultParameter = parameterMap[index]
                    }
                    overridingFunctions.forEach { it.propagateDefaultParameters(parameterMap.toMutableMap()) }
                }

                override fun visitElement(element: IrElement): Unit = element.acceptChildrenVoid(this)

                override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                    if (declaration.symbol in visitedFunctions) return
                    val rootFunction = declaration.rootOverriddenFunction
                    rootFunction.propagateDefaultParameters()
                }

                override fun visitConstructor(declaration: IrConstructor) {
                    // Probably not necessary
                    if (declaration.symbol in visitedFunctions) return
                    // Constructors cannot be overridden, so the default parameters can come only from the actual declaration itself
                    declaration.parameters.forEach {
                        if (it.defaultValue != null) it.closestOverriddenDefaultParameter = it.symbol
                    }
                }
            })
        }
    }
}
