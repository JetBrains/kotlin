/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.persistent.carriers

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody

internal interface EnumEntryCarrier : DeclarationCarrier {
    var correspondingClassField: IrClass?
    var initializerExpressionField: IrExpressionBody?

    override fun clone(): EnumEntryCarrier {
        return EnumEntryCarrierImpl(
            lastModified,
            parentField,
            originField,
            annotationsField,
            correspondingClassField,
            initializerExpressionField
        )
    }
}

internal class EnumEntryCarrierImpl(
    override val lastModified: Int,
    override var parentField: IrDeclarationParent?,
    override var originField: IrDeclarationOrigin,
    override var annotationsField: List<IrConstructorCall>,
    override var correspondingClassField: IrClass?,
    override var initializerExpressionField: IrExpressionBody?
) : EnumEntryCarrier
