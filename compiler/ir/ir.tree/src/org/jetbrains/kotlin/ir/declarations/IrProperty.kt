/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol

interface IrProperty :
    IrDeclarationWithName,
    IrDeclarationWithVisibility,
    IrSymbolOwner {

    override val descriptor: PropertyDescriptor
    override val symbol: IrPropertySymbol

    val modality: Modality
    val isVar: Boolean
    val isConst: Boolean
    val isLateinit: Boolean
    val isDelegated: Boolean
    val isExternal: Boolean
    val isExpect: Boolean
    val isFakeOverride: Boolean

    var backingField: IrField?
    var getter: IrSimpleFunction?
    var setter: IrSimpleFunction?

    override var metadata: MetadataSource?
}