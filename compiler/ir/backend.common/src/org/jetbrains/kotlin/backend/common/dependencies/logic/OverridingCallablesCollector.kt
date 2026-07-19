/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.dependencies.logic

import org.jetbrains.kotlin.backend.common.dependencies.util.traversal
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.Modality
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

private var <S : IrBindableSymbol<*, D>, D : IrOverridableDeclaration<S>> D.overridingCallables: MutableSet<S>? by irAttribute(copyByDefault = false)

val <S : IrBindableSymbol<*, D>, D : IrOverridableDeclaration<S>> S.overridingCallables: Set<S>
    get() = owner.overridingCallables ?: emptySet()

val IrPropertySymbol.overridingProperties: Set<IrPropertySymbol> get() = overridingCallables

val IrSimpleFunctionSymbol.overridingFunctions: Set<IrSimpleFunctionSymbol> get() = overridingCallables

fun <S : IrBindableSymbol<*, D>, D : IrOverridableDeclaration<S>> D.realOverridden(distinct: Boolean = false): Sequence<S> =
    traversal(this) { decl ->
        if (!decl.isFakeOverride) emit(decl.symbol)
        else decl.overriddenSymbols.forEach { traverseFor(it.owner) }
    }.let { if (distinct) it.distinct() else it }

fun <S : IrBindableSymbol<*, D>, D : IrOverridableDeclaration<S>> S.realOverridden(): Sequence<S> = owner.realOverridden()

fun <S : IrBindableSymbol<*, D>, D : IrOverridableDeclaration<S>> S.overrides(): Sequence<S> = traversal(owner) { decl ->
    decl.overridingCallables?.forEach {
        if (it.owner.modality != Modality.ABSTRACT) emit(it)
        traverseFor(it.owner)
    }
}

object OverridingCallablesCollector : IrGenerationExtension {

    private fun <S : IrBindableSymbol<*, D>, D : IrOverridableDeclaration<S>> D.addOverrideToOverriddenCallables() {
        overriddenSymbols.forEach { overridden ->
            overridden.owner.overridingCallables = overridden.owner.overridingCallables?.apply { this += symbol }
                ?: mutableSetOf(symbol)
        }
    }

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.files.forEach { file ->
            file.acceptVoid(object : IrVisitorVoid() {

                override fun visitElement(element: IrElement): Unit = element.acceptChildrenVoid(this)

                override fun visitSimpleFunction(declaration: IrSimpleFunction) = declaration.addOverrideToOverriddenCallables()

                override fun visitProperty(declaration: IrProperty) = declaration.addOverrideToOverriddenCallables()
            })
        }
    }
}
