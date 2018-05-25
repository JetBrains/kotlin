/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.types.IrType

interface TypeRemapper {
    fun enterScope(irTypeParametersContainer: IrTypeParametersContainer)
    fun remapType(type: IrType): IrType
    fun leaveScope()
}

inline fun <T> TypeRemapper.withinScope(irTypeParametersContainer: IrTypeParametersContainer, fn: () -> T): T {
    enterScope(irTypeParametersContainer)
    val result = fn()
    leaveScope()
    return result
}