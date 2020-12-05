/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

val IrDeclaration.isReal: Boolean get() = !isFakeOverride

val IrDeclaration.isFakeOverride: Boolean
    get() = when (this) {
        is IrSimpleFunction -> isFakeOverride
        is IrProperty -> isFakeOverride
        else -> false
    }

val IrSimpleFunction.target: IrSimpleFunction
    get() = if (modality == Modality.ABSTRACT)
        this
    else
        resolveFakeOverride() ?: error("Could not resolveFakeOverride() for ${this.render()}")

val IrFunction.target: IrFunction
    get() = when (this) {
        is IrSimpleFunction -> this.target
        is IrConstructor -> this
        else -> error(this)
    }

fun IrSimpleFunction.collectRealOverrides(filter: (IrOverridableMember) -> Boolean = { false }): Set<IrSimpleFunction> {
    if (isReal) return setOf(this)

    return this.overriddenSymbols
        .map { it.owner }
        .collectAndFilterRealOverrides(filter)
        .map { it as IrSimpleFunction }
        .toSet()
}

fun Collection<IrOverridableMember>.collectAndFilterRealOverrides(
    filter: (IrOverridableMember) -> Boolean = { false }
): Set<IrOverridableMember> {
    val visited = mutableSetOf<IrOverridableMember>()
    val realOverrides = mutableSetOf<IrOverridableMember>()

    fun overriddenSymbols(declaration: IrOverridableMember) = when (declaration) {
        is IrSimpleFunction -> declaration.overriddenSymbols
        is IrProperty -> (declaration.getter ?: declaration.setter)
            ?.overriddenSymbols?.mapNotNull { it.owner.correspondingPropertySymbol }
            ?: emptyList()
        else -> error("Unexpected overridable member: ${declaration.render()}")
    }

    fun collectRealOverrides(member: IrOverridableMember) {
        if (!visited.add(member) || filter(member)) return

        if (member.isReal) {
            realOverrides += member
        } else {
            overriddenSymbols(member).forEach { collectRealOverrides(it.owner as IrOverridableMember) }
        }
    }

    this.forEach { collectRealOverrides(it) }

    fun excludeRepeated(member: IrOverridableMember) {
        if (!visited.add(member)) return

        overriddenSymbols(member).forEach {
            realOverrides.remove(it.owner)
            excludeRepeated(it.owner as IrOverridableMember)
        }
    }

    visited.clear()
    realOverrides.toList().forEach { excludeRepeated(it) }

    return realOverrides
}

// TODO: use this implementation instead of any other
fun IrSimpleFunction.resolveFakeOverride(allowAbstract: Boolean = false): IrSimpleFunction? {
    return if (allowAbstract) {
        val reals = collectRealOverrides()
        if (reals.isEmpty()) error("No real overrides for ${this.render()}")
        reals.first()
    } else {
        collectRealOverrides { it.modality == Modality.ABSTRACT }
            .let { realOverrides ->
                // Kotlin forbids conflicts between overrides, but they may trickle down from Java.
                realOverrides.singleOrNull { it.parent.safeAs<IrClass>()?.isInterface != true }
                // TODO: We take firstOrNull instead of singleOrNull here because of KT-36188.
                    ?: realOverrides.firstOrNull()
            }
    }
}
