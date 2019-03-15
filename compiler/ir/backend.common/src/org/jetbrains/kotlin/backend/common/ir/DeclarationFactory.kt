/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.IrType

interface DeclarationFactory {
    object FIELD_FOR_OUTER_THIS : IrDeclarationOriginImpl("FIELD_FOR_OUTER_THIS")

    fun getFieldForEnumEntry(enumEntry: IrEnumEntry, type: IrType): IrField
    fun getOuterThisField(innerClass: IrClass): IrField
    fun getInnerClassConstructorWithOuterThisParameter(innerClassConstructor: IrConstructor): IrConstructor
    fun getFieldForObjectInstance(singleton: IrClass): IrField
}