/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.persistent.carriers

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.types.IrType

internal interface ClassCarrier : DeclarationCarrier {
    var thisReceiverField: IrValueParameter?
    var metadataField: MetadataSource?
    var visibilityField: DescriptorVisibility
    var modalityField: Modality
    var attributeOwnerIdField: IrAttributeContainer
    var typeParametersField: List<IrTypeParameter>
    var superTypesField: List<IrType>

    override fun clone(): ClassCarrier {
        return ClassCarrierImpl(
            lastModified,
            parentField,
            originField,
            annotationsField,
            thisReceiverField,
            metadataField,
            visibilityField,
            modalityField,
            attributeOwnerIdField,
            typeParametersField,
            superTypesField
        )
    }
}

internal class ClassCarrierImpl(
    override val lastModified: Int,
    override var parentField: IrDeclarationParent?,
    override var originField: IrDeclarationOrigin,
    override var annotationsField: List<IrConstructorCall>,
    override var thisReceiverField: IrValueParameter?,
    override var metadataField: MetadataSource?,
    override var visibilityField: DescriptorVisibility,
    override var modalityField: Modality,
    override var attributeOwnerIdField: IrAttributeContainer,
    override var typeParametersField: List<IrTypeParameter>,
    override var superTypesField: List<IrType>
) : ClassCarrier
