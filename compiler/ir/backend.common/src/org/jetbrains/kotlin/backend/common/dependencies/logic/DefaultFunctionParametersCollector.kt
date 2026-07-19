/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.dependencies.logic

import org.jetbrains.kotlin.backend.common.dependencies.util.PathCompressingAncestorMap
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

private var IrValueParameter.closestOverriddenDefaultParameter: IrValueParameterSymbol? by irAttribute(copyByDefault = false)

val IrValueParameterSymbol.closestOverriddenDefaultParameter: IrValueParameterSymbol? get() = owner.closestOverriddenDefaultParameter

object DefaultFunctionParametersCollector : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.files.forEach { file ->
            file.acceptVoid(object : IrVisitorVoid() {

                private val rootOverriddenFunctionFinder = PathCompressingAncestorMap<IrSimpleFunctionSymbol> {
                    it.owner.overriddenSymbols.asSequence()
                }

                private val visitedFunctions = mutableSetOf<IrFunctionSymbol>()

                private val IrSimpleFunction.rootOverriddenFunction: IrSimpleFunctionSymbol
                    get() = rootOverriddenFunctionFinder[symbol]

                private fun IrSimpleFunctionSymbol.propagateDefaultParameters(parameterMap: MutableMap<Int, IrValueParameterSymbol> = mutableMapOf()) {
                    if (!visitedFunctions.add(this)) return
                    owner.parameters.forEachIndexed { index, parameter ->
                        if (parameter.defaultValue != null) parameterMap[index] = parameter.symbol
                        if (index in parameterMap) parameter.closestOverriddenDefaultParameter = parameterMap[index]
                    }
                    overridingFunctions.forEach { it.propagateDefaultParameters(parameterMap.toMutableMap()) }
                }

                override fun visitElement(element: IrElement): Unit = element.acceptChildrenVoid(this)

                override fun visitFile(declaration: IrFile) {
                    super.visitFile(declaration)
                    visitedFunctions.clear()
                    rootOverriddenFunctionFinder.reset()
                }

                override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                    if (declaration.symbol in visitedFunctions) return
                    val rootFunction = declaration.rootOverriddenFunction
                    rootFunction.propagateDefaultParameters()
                }

                override fun visitConstructor(declaration: IrConstructor) {
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
