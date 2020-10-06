/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.IrType

abstract class IrValueDeclaration : IrDeclarationBase(), IrDeclarationWithName, IrSymbolOwner {
    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: ValueDescriptor

    abstract override val symbol: IrValueSymbol
    abstract var type: IrType
}
