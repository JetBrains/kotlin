/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrBindableSymbol
import org.jetbrains.kotlin.ir.util.isReal
import org.jetbrains.kotlin.ir.util.original

// We may get several real supers here (e.g. see the code snippet from KT-33034).
// TODO: Consider reworking the resolution algorithm to get a determined super declaration.
private fun <S: IrBindableSymbol<*, D>, D: IrOverridableDeclaration<S>> D.getRealSupers(): Set<D> {
    if (this.isReal) {
        return setOf(this)
    }

    val visited = mutableSetOf<D>()
    val realSupers = mutableSetOf<D>()

    fun findRealSupers(declaration: D) {
        if (declaration in visited) return
        visited += declaration
        if (declaration.isReal) {
            realSupers += declaration
        } else {
            declaration.overriddenSymbols.forEach { findRealSupers(it.owner) }
        }
    }

    findRealSupers(this)

    if (realSupers.size > 1) {
        visited.clear()

        fun excludeOverridden(declaration: D) {
            if (declaration in visited) return
            visited += declaration
            declaration.overriddenSymbols.forEach {
                realSupers.remove(it.owner)
                excludeOverridden(it.owner)
            }
        }

        realSupers.toList().forEach { excludeOverridden(it) }
    }

    return realSupers
}

/**
 * Implementation of given method.
 *
 * TODO: this method is actually a part of resolve and probably duplicates another one
 */
fun IrSimpleFunction.resolveFakeOverride(allowAbstract: Boolean = false): IrSimpleFunction {
    val realSupers = getRealSupers()

    return if (allowAbstract) {
        realSupers.first()
    } else {
        realSupers.single { it.modality != Modality.ABSTRACT }
    }
}

/**
 * Implementation of given method.
 *
 * TODO: this method is actually a part of resolve and probably duplicates another one
 */
internal fun IrSimpleFunction.resolveFakeOverrideMaybeAbstract() = this.resolveFakeOverride(allowAbstract = true)

internal fun IrProperty.resolveFakeOverrideMaybeAbstract(): IrProperty =
    this.getter!!.resolveFakeOverrideMaybeAbstract().correspondingPropertySymbol!!.owner

/**
 * TODO: This method can be simplified if the "overriddenSymbols" list of an IrField always includes only one element.
 * Currently it's not the case for some cinterop libraries (e.g. Foundation). In these libraries interfaces containing
 * final properties with external getters are created (corresponding compiler error is suppressed). Due to KT-33081
 * such a property gets a backing field and a field of a class implementing such interfaces can have several elements
 * in the overriddenSymbols list.
 */
internal fun IrField.resolveFakeOverride(): IrField = getRealSupers().first()

val IrSimpleFunction.target: IrSimpleFunction
    get() = (if (modality == Modality.ABSTRACT) this else resolveFakeOverride()).original

val IrFunction.target: IrFunction get() = when (this) {
    is IrSimpleFunction -> this.target
    is IrConstructor -> this
    else -> error(this)
}
