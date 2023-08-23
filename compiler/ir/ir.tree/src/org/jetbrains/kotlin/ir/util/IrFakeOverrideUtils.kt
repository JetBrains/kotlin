/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.*
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
        else -> error(this)
    }

fun <S : IrSymbol, T : IrOverridableDeclaration<S>> T.collectRealOverrides(
    toSkip: (T) -> Boolean = { false },
    filter: (T) -> Boolean = { false },
): Set<T> {
    if (isReal && !toSkip(this)) return setOf(this)

    @Suppress("UNCHECKED_CAST")
    return this.overriddenSymbols
        .map { it.owner as T }
        .collectAndFilterRealOverrides(toSkip, filter)
}

fun <S : IrSymbol, T : IrOverridableDeclaration<S>> Collection<T>.collectAndFilterRealOverrides(
    toSkip: (T) -> Boolean = { false },
    filter: (T) -> Boolean = { false }
): Set<T> {

    val visited = mutableSetOf<T>()
    val realOverrides = mutableMapOf<Any, T>()

    /*
        Due to IR copying in performByIrFile, overrides should only be distinguished up to their signatures.
     */
    fun T.toKey(): Any = symbol.signature ?: this

    fun collectRealOverrides(member: T) {
        if (!visited.add(member) || filter(member)) return

        if (member.isReal && !toSkip(member)) {
            realOverrides[member.toKey()] = member
        } else {
            @Suppress("UNCHECKED_CAST")
            member.overriddenSymbols.forEach { collectRealOverrides(it.owner as T) }
        }
    }

    this.forEach { collectRealOverrides(it) }

    fun excludeRepeated(member: T) {
        if (!visited.add(member)) return

        member.overriddenSymbols.forEach {
            @Suppress("UNCHECKED_CAST")
            val owner = it.owner as T
            realOverrides.remove(owner.toKey())
            excludeRepeated(owner)
        }
    }

    visited.clear()
    realOverrides.toList().forEach { excludeRepeated(it.second) }

    return realOverrides.values.toSet()
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
    toSkip: (T) -> Boolean = { false },
): T? {
    if (!isFakeOverride && !toSkip(this)) return this
    return collectRealOverrides(toSkip).firstOrNull()
}

fun <S : IrSymbol, T : IrOverridableDeclaration<S>> T.resolveFakeOverrideOrFail(): T =
    resolveFakeOverride() ?: error("No real overrides for ${this.render()}")

// TODO: use this implementation instead of any other
fun <S : IrSymbol, T : IrOverridableDeclaration<S>> T.resolveFakeOverride(
    toSkip: (T) -> Boolean = { false },
): T? {
    if (!isFakeOverride && !toSkip(this)) return this
    return collectRealOverrides(toSkip) { it.modality == Modality.ABSTRACT }
        .let { realOverrides ->
            // Kotlin forbids conflicts between overrides, but they may trickle down from Java.
            realOverrides.singleOrNull { (it.parent as? IrClass)?.isInterface != true }
            // TODO: We take firstOrNull instead of singleOrNull here because of KT-36188.
                ?: realOverrides.firstOrNull()
        }
}
