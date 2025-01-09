/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol

val IrDeclaration.isReal: Boolean get() = !isFakeOverride

val IrDeclaration.isFakeOverride: Boolean
    get() = when (this) {
        is IrSimpleFunction -> isFakeOverride
        is IrProperty -> isFakeOverride
        else -> false
    }

val IrSimpleFunction.target: IrSimpleFunction
    get() = if (modality == Modality.ABSTRACT) this else resolveFakeOverrideOrFail()

val IrFunction.target: IrFunction
    get() = when (this) {
        is IrSimpleFunction -> this.target
        is IrConstructor -> this
    }

val IrFunctionAccessExpression.isVirtualCall: Boolean
    get() = this is IrCall && this.superQualifierSymbol == null && this.symbol.owner.isOverridable

val IrFunctionAccessExpression.target: IrFunction
    get() = this.symbol.owner.let { (it as? IrSimpleFunction)?.takeUnless { this.isVirtualCall }?.target ?: it }

fun <T : IrOverridableDeclaration<*>> T.collectRealOverrides(
    isFakeOverride: (T) -> Boolean = IrOverridableDeclaration<*>::isFakeOverride,
    filter: (T) -> Boolean = { false },
): Set<T> {
    if (!isFakeOverride(this)) return setOf(this)

    @Suppress("UNCHECKED_CAST")
    return this.overriddenSymbols
        .map { it.owner as T }
        .collectAndFilterRealOverrides(isFakeOverride, filter)
}

private fun <T : IrOverridableDeclaration<*>> Collection<T>.collectAndFilterRealOverrides(
    isFakeOverride: (T) -> Boolean = IrOverridableDeclaration<*>::isFakeOverride,
    filter: (T) -> Boolean = { false }
): Set<T> {
    val visited = mutableSetOf<T>()
    val realOverrides = mutableSetOf<T>()

    fun collectRealOverrides(member: T) {
        if (!visited.add(member) || filter(member)) return

        if (!isFakeOverride(member)) {
            realOverrides += member
        } else {
            @Suppress("UNCHECKED_CAST")
            for (overridden in member.overriddenSymbols) {
                collectRealOverrides(overridden.owner as T)
            }
        }
    }

    for (declaration in this) {
        collectRealOverrides(declaration)
    }

    fun excludeRepeated(member: T) {
        if (!visited.add(member)) return

        for (overridden in member.overriddenSymbols) {
            @Suppress("UNCHECKED_CAST")
            val owner = overridden.owner as T
            realOverrides.remove(owner)
            excludeRepeated(owner)
        }
    }

    visited.clear()
    for (realOverride in realOverrides.toList()) {
        excludeRepeated(realOverride)
    }

    return realOverrides
}

@Suppress("UNCHECKED_CAST")
fun Collection<IrOverridableMember>.collectAndFilterRealOverrides(): Set<IrOverridableMember> = when {
    all { it is IrSimpleFunction } -> (this as Collection<IrSimpleFunction>).collectAndFilterRealOverrides()
    all { it is IrProperty } -> (this as Collection<IrProperty>).collectAndFilterRealOverrides()
    else -> error("all members should be of the same kind, got ${map { it.render() }}")
}

fun <S : IrSymbol, T : IrOverridableDeclaration<S>> T.resolveFakeOverrideMaybeAbstractOrFail(): T =
    resolveFakeOverrideMaybeAbstract() ?: error("No real overrides for ${this.render()}")

fun <S : IrSymbol, T : IrOverridableDeclaration<S>> T.resolveFakeOverrideMaybeAbstract(
    isFakeOverride: (T) -> Boolean = IrOverridableDeclaration<*>::isFakeOverride,
): T? {
    if (!isFakeOverride(this)) return this
    return collectRealOverrides(isFakeOverride).firstOrNull()
}

fun <S : IrSymbol, T : IrOverridableDeclaration<S>> T.resolveFakeOverrideOrFail(): T =
    resolveFakeOverride() ?: error("No real overrides for ${this.render()}")

// TODO: use this implementation instead of any other
fun <T : IrOverridableDeclaration<*>> T.resolveFakeOverride(
    isFakeOverride: (T) -> Boolean = IrOverridableDeclaration<*>::isFakeOverride,
): T? {
    if (!isFakeOverride(this)) return this
    return collectRealOverrides(isFakeOverride) { it.modality == Modality.ABSTRACT }
        .let { realOverrides ->
            // Kotlin forbids conflicts between overrides, but they may trickle down from Java.
            realOverrides.singleOrNull { (it.parent as? IrClass)?.isInterface != true }
            // TODO: We take firstOrNull instead of singleOrNull here because of KT-36188.
                ?: realOverrides.firstOrNull()
        }
}
