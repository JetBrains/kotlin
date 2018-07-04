/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

interface IrField : IrSymbolDeclaration<IrFieldSymbol> {
    override val descriptor: PropertyDescriptor

    val name: Name
    val type: IrType
    val visibility: Visibility
    val isFinal: Boolean
    val isExternal: Boolean

    var initializer: IrExpressionBody?
}