/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType

interface IrField : IrSymbolDeclaration<IrFieldSymbol> {
    override val descriptor: PropertyDescriptor

    override val declarationKind: IrDeclarationKind
        get() = IrDeclarationKind.FIELD

    val name: Name
    val type: KotlinType
    val visibility: Visibility
    var initializer: IrExpressionBody?
}