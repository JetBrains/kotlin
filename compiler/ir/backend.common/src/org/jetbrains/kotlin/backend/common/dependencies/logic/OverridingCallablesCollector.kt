/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.dependencies.logic

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

private var IrProperty.overridingProperties: List<IrPropertySymbol>? by irAttribute(copyByDefault = false)

val IrPropertySymbol.overridingProperties: List<IrPropertySymbol> get() = owner.overridingProperties ?: emptyList()

private var IrSimpleFunction.overridingFunctions: List<IrSimpleFunctionSymbol>? by irAttribute(copyByDefault = false)

val IrSimpleFunctionSymbol.overridingFunctions: List<IrSimpleFunctionSymbol> get() = owner.overridingFunctions ?: emptyList()

object OverridingCallablesCollector : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.files.forEach { file ->
            file.acceptVoid(object : IrVisitorVoid() {

                override fun visitElement(element: IrElement): Unit = element.acceptChildrenVoid(this)

                override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                    val symbol = declaration.symbol
                    declaration.overriddenSymbols.forEach {
                        it.owner.overridingFunctions = it.owner.overridingFunctions?.plus(symbol) ?: listOf(symbol)
                    }
                }

                override fun visitProperty(declaration: IrProperty) {
                    val symbol = declaration.symbol
                    declaration.overriddenSymbols.forEach {
                        it.owner.overridingProperties = it.owner.overridingProperties?.plus(symbol) ?: listOf(symbol)
                    }
                }
            })
        }
    }
}
