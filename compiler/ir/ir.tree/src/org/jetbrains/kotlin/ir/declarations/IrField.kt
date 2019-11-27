/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.types.IrType

interface IrField :
    IrSymbolDeclaration<IrFieldSymbol>, IrOverridableDeclaration<IrFieldSymbol>,
    IrDeclarationWithName, IrDeclarationWithVisibility, IrDeclarationParent {

    override val descriptor: PropertyDescriptor

    val type: IrType
    val isFinal: Boolean
    val isExternal: Boolean
    val isStatic: Boolean
    val isFakeOverride: Boolean

    var initializer: IrExpressionBody?

    var correspondingPropertySymbol: IrPropertySymbol?

    override val metadata: MetadataSource.Property?
}
