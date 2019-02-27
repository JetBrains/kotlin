/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.*


/**
 * Implementation of given method.
 *
 * TODO: this method is actually a part of resolve and probably duplicates another one
 */
internal fun IrSimpleFunction.resolveFakeOverride(allowAbstract: Boolean = false): IrSimpleFunction {
    if (this.isReal) {
        return this
    }

    val visited = mutableSetOf<IrSimpleFunction>()
    val realSupers = mutableSetOf<IrSimpleFunction>()

    fun findRealSupers(function: IrSimpleFunction) {
        if (function in visited) return
        visited += function
        if (function.isReal) {
            realSupers += function
        } else {
            function.overriddenSymbols.forEach { findRealSupers(it.owner) }
        }
    }

    findRealSupers(this)

    if (realSupers.size > 1) {
        visited.clear()

        fun excludeOverridden(function: IrSimpleFunction) {
            if (function in visited) return
            visited += function
            function.overriddenSymbols.forEach {
                realSupers.remove(it.owner)
                excludeOverridden(it.owner)
            }
        }

        realSupers.toList().forEach { excludeOverridden(it) }
    }

    return realSupers.first { allowAbstract || it.modality != Modality.ABSTRACT }
}

internal val IrClass.isInterface: Boolean
    get() = (this.kind == ClassKind.INTERFACE)

internal val IrSimpleFunction.target: IrSimpleFunction
    get() = (if (modality == Modality.ABSTRACT) this else resolveFakeOverride()).original

internal val IrFunction.target: IrFunction get() = when (this) {
    is IrSimpleFunction -> this.target
    is IrConstructor -> this
    else -> error(this)
}

val IrDeclaration.isPropertyAccessor get() =
    this is IrSimpleFunction && this.correspondingProperty != null

val IrDeclaration.isPropertyField get() =
    this is IrField && this.correspondingProperty != null

val IrDeclaration.isTopLevelDeclaration get() =
    parent !is IrDeclaration && !this.isPropertyAccessor && !this.isPropertyField

fun IrDeclaration.findTopLevelDeclaration(): IrDeclaration = when {
    this.isTopLevelDeclaration ->
        this
    this.isPropertyAccessor ->
        (this as IrSimpleFunction).correspondingProperty!!.findTopLevelDeclaration()
    this.isPropertyField ->
        (this as IrField).correspondingProperty!!.findTopLevelDeclaration()
    else ->
        (this.parent as IrDeclaration).findTopLevelDeclaration()
}