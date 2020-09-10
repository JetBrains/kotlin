/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.persistent.carriers

import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.types.IrType

internal interface ValueParameterCarrier : DeclarationCarrier {
    var defaultValueField: IrExpressionBody?
    var typeField: IrType

    override fun clone(): ValueParameterCarrier {
        return ValueParameterCarrierImpl(
            lastModified, parentField, originField, annotationsField, defaultValueField, typeField
        )
    }
}

internal class ValueParameterCarrierImpl(
    override val lastModified: Int,
    override var parentField: IrDeclarationParent?,
    override var originField: IrDeclarationOrigin,
    override var annotationsField: List<IrConstructorCall>,
    override var defaultValueField: IrExpressionBody?,
    override var typeField: IrType,
) : ValueParameterCarrier
