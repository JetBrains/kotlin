/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.persistent.carriers

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall

internal interface PropertyCarrier : DeclarationCarrier {
    var backingFieldField: IrField?
    var getterField: IrSimpleFunction?
    var setterField: IrSimpleFunction?
    var metadataField: MetadataSource?
    var attributeOwnerIdField: IrAttributeContainer

    override fun clone(): PropertyCarrier {
        return PropertyCarrierImpl(
            lastModified,
            parentField,
            originField,
            annotationsField,
            backingFieldField,
            getterField,
            setterField,
            metadataField,
            attributeOwnerIdField,
        )
    }
}

internal class PropertyCarrierImpl(
    override val lastModified: Int,
    override var parentField: IrDeclarationParent?,
    override var originField: IrDeclarationOrigin,
    override var annotationsField: List<IrConstructorCall>,
    override var backingFieldField: IrField?,
    override var getterField: IrSimpleFunction?,
    override var setterField: IrSimpleFunction?,
    override var metadataField: MetadataSource?,
    override var attributeOwnerIdField: IrAttributeContainer
) : PropertyCarrier
