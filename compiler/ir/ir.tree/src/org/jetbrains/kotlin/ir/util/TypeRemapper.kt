/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.types.IrType

interface TypeRemapperWithData<in D> {
    fun enterScope(irTypeParametersContainer: IrTypeParametersContainer, data: D)
    fun remapType(type: IrType, data: D): IrType
    fun leaveScope(data: D)
}

interface TypeRemapper : TypeRemapperWithData<Nothing?> {
    fun enterScope(irTypeParametersContainer: IrTypeParametersContainer)

    override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer, data: Nothing?) {
        enterScope(irTypeParametersContainer)
    }

    fun remapType(type: IrType): IrType

    override fun remapType(type: IrType, data: Nothing?): IrType = remapType(type)

    fun leaveScope()

    override fun leaveScope(data: Nothing?) {
        leaveScope()
    }
}

inline fun <T> TypeRemapper.withinScope(irTypeParametersContainer: IrTypeParametersContainer, fn: () -> T): T {
    enterScope(irTypeParametersContainer)
    val result = fn()
    leaveScope()
    return result
}
