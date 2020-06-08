/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.ir.DescriptorBasedIr
import org.jetbrains.kotlin.ir.symbols.IrTypeAliasSymbol
import org.jetbrains.kotlin.ir.types.IrType

interface IrTypeAlias :
    IrSymbolDeclaration<IrTypeAliasSymbol>,
    IrDeclarationWithName,
    IrDeclarationWithVisibility,
    IrTypeParametersContainer {

    @DescriptorBasedIr
    override val descriptor: TypeAliasDescriptor

    val isActual: Boolean
    val expandedType: IrType
}