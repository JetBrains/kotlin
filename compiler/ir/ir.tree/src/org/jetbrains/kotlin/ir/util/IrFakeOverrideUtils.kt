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

val IrFunction.target: IrFunction get() = when (this) {
    is IrSimpleFunction -> this.target
    is IrConstructor -> this
    else -> error(this)
}

fun IrSimpleFunction.collectRealOverrides(toSkip: (IrSimpleFunction) -> Boolean = { false }): Set<IrSimpleFunction> {
    if (isReal && !toSkip(this)) return setOf(this)

    val visited = mutableSetOf<IrSimpleFunction>()
    val realOverrides = mutableSetOf<IrSimpleFunction>()

    fun collectRealOverrides(func: IrSimpleFunction) {
        if (!visited.add(func)) return

        if (func.isReal && !toSkip(func)) {
            realOverrides += func
        } else {
            func.overriddenSymbols.forEach { collectRealOverrides(it.owner) }
        }
    }

    overriddenSymbols.forEach { collectRealOverrides(it.owner) }

    fun excludeRepeated(func: IrSimpleFunction) {
        if (!visited.add(func)) return

        func.overriddenSymbols.forEach {
            realOverrides.remove(it.owner)
            excludeRepeated(it.owner)
        }
    }

    visited.clear()
    realOverrides.toList().forEach { excludeRepeated(it) }

    return realOverrides
}

// TODO: use this implementation instead of any other
fun IrSimpleFunction.resolveFakeOverride(allowAbstract: Boolean = false, toSkip: (IrSimpleFunction) -> Boolean = { false }): IrSimpleFunction? {
    val reals = collectRealOverrides(toSkip)
    return if (allowAbstract) {
        if (reals.isEmpty()) error("No real overrides for ${this.render()}")
        reals.first()
    } else {
        reals
            .filter { it.modality != Modality.ABSTRACT }
            .let { realOverrides ->
                // Kotlin forbids conflicts between overrides, but they may trickle down from Java.
                realOverrides.singleOrNull { it.parent.safeAs<IrClass>()?.isInterface != true }
                // TODO: We take firstOrNull instead of singleOrNull here because of KT-36188.
                    ?: realOverrides.firstOrNull()
            }
    }
}
