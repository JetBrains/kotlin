/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.dependencies.logic

import org.jetbrains.kotlin.backend.common.dependencies.util.traversal
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrOverridableDeclaration
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.IrBindableSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import kotlin.collections.forEach

private var IrProperty.overridingProperties: MutableSet<IrPropertySymbol>? by irAttribute(copyByDefault = false)

val IrPropertySymbol.overridingProperties: Set<IrPropertySymbol> get() = owner.overridingProperties ?: emptySet()

private var IrSimpleFunction.overridingFunctions: MutableSet<IrSimpleFunctionSymbol>? by irAttribute(copyByDefault = false)

val IrSimpleFunctionSymbol.overridingFunctions: Set<IrSimpleFunctionSymbol> get() = owner.overridingFunctions ?: emptySet()

object OverridingCallablesCollector : IrGenerationExtension {

    private fun <S : IrBindableSymbol<*, D>, D : IrOverridableDeclaration<S>> D.realOverrides(): Sequence<S> = traversal(this) {
        if (!isFakeOverride) emit(symbol)
        else overriddenSymbols.forEach { traverseFor(it.owner) }
    }

    private inline fun <S : IrBindableSymbol<*, D>, D : IrOverridableDeclaration<S>> D.addOverrideToOverriddenCallables(
        overridingCallablesGetter: D.() -> MutableSet<S>?,
        overridingCallablesSetter: D.(MutableSet<S>) -> Unit
    ) {
        if (isFakeOverride) return
        overriddenSymbols.flatMap { it.owner.realOverrides() }.distinct().forEach { overridden ->
            overridden.owner.overridingCallablesSetter(
                overridden.owner.overridingCallablesGetter()?.apply { this += symbol } ?: mutableSetOf(symbol)
            )
        }
    }

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.files.forEach { file ->
            file.acceptVoid(object : IrVisitorVoid() {

                override fun visitElement(element: IrElement): Unit = element.acceptChildrenVoid(this)

                override fun visitSimpleFunction(declaration: IrSimpleFunction) =
                    declaration.addOverrideToOverriddenCallables(
                        overridingCallablesGetter = { overridingFunctions },
                        overridingCallablesSetter = { overridingFunctions = it }
                    )

                override fun visitProperty(declaration: IrProperty) =
                    declaration.addOverrideToOverriddenCallables(
                        overridingCallablesGetter = { overridingProperties },
                        overridingCallablesSetter = { overridingProperties = it }
                    )
            })
        }
    }
}
