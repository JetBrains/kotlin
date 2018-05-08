/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.descriptors

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrEnumEntrySymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol

interface DescriptorsFactory {
    fun getSymbolForEnumEntry(enumEntry: IrEnumEntrySymbol): IrFieldSymbol
    fun getOuterThisFieldSymbol(innerClass: IrClass): IrFieldSymbol
    fun getInnerClassConstructorWithOuterThisParameter(innerClassConstructor: IrConstructor): IrConstructorSymbol
    fun getSymbolForObjectInstance(singleton: IrClassSymbol): IrFieldSymbol
}