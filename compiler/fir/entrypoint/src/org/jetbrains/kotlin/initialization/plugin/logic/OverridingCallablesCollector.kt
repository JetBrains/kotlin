/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.initialization.plugin.logic

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.initialization.plugin.util.traversal
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

private var <S : IrBindableSymbol<*, D>, D : IrOverridableDeclaration<S>> D.overridingCallables: MutableSet<S>? by irAttribute(copyByDefault = false)

val <S : IrBindableSymbol<*, D>, D : IrOverridableDeclaration<S>> S.overridingCallables: Set<S>? get() = owner.overridingCallables

val IrPropertySymbol.overridingProperties: Set<IrPropertySymbol>? get() = overridingCallables

val IrSimpleFunctionSymbol.overridingFunctions: Set<IrSimpleFunctionSymbol>? get() = overridingCallables

fun <S : IrBindableSymbol<*, D>, D : IrOverridableDeclaration<S>> D.realOverridden(): Sequence<S> =
    traversal(this) { decl ->
        if (!decl.isFakeOverride) emit(decl.symbol)
        else decl.overriddenSymbols.forEach { traverseFor(it.owner) }
    }

fun <S : IrBindableSymbol<*, D>, D : IrOverridableDeclaration<S>> S.realOverridden(): Sequence<S> = owner.realOverridden()

fun <S : IrBindableSymbol<*, D>, D : IrOverridableDeclaration<S>> D.overrides(): Sequence<S> =
    if (isFakeOverride) realOverridden().distinct().flatMap { it.owner.overrides() }.distinct()
    else traversal(this) { decl ->
        decl.overridingCallables?.forEach {
            if (it.owner.modality != Modality.ABSTRACT) emit(it)
            traverseFor(it.owner)
        }
    }

fun <S : IrBindableSymbol<*, D>, D : IrOverridableDeclaration<S>> S.overrides(): Sequence<S> = owner.overrides()

object OverridingCallablesCollector : IrGenerationExtension {

    private fun <S : IrBindableSymbol<*, D>, D : IrOverridableDeclaration<S>> D.addOverrideToOverriddenCallables() {
        if (isFakeOverride) return
        else {
            overridingCallables = mutableSetOf()
            overriddenSymbols.flatMap { it.realOverridden() }.distinct().forEach { overridden ->
                overridden.owner.overridingCallables = overridden.owner.overridingCallables?.apply { this += symbol }
                    ?: mutableSetOf(symbol)
            }
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
