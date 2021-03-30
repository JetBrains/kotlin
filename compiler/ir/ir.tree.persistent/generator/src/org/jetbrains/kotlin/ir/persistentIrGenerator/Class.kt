/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.persistentIrGenerator

internal fun PersistentIrGenerator.generateClass() {
    val visibilityField = Field("visibility", DescriptorVisibility, visibilityProto)
    val thisReceiverField = Field(
        "thisReceiver",
        IrValueParameter + "?",
        valueParameterProto,
        propSymbolType = IrValueParameterSymbol + "?",
        symbolToDeclaration = +"?.owner",
        declarationToSymbol = +"?.symbol"
    )
    val superTypesField = Field("superTypes", +"List<" + import("IrType", "org.jetbrains.kotlin.ir.types") + ">", superTypeListProto)
    val modalityField = Field("modality", descriptorType("Modality"), modalityProto)
    val inlineClassRepresentationField = Field(
        "inlineClassRepresentation",
        descriptorType("InlineClassRepresentation") + "<" + import("IrSimpleType", "org.jetbrains.kotlin.ir.types") + ">?",
        inlineClassRepresentationProto
    )

    writeFile("PersistentIrClass.kt", renderFile("org.jetbrains.kotlin.ir.declarations.persistent") {
        lines(
            id,
            +"internal class PersistentIrClass(",
            arrayOf(
                startOffset,
                endOffset,
                origin,
                +"override val symbol: " + IrClassSymbol,
                name,
                kind,
                visibility,
                modality,
                isCompanion,
                isInner,
                isData,
                isExternal + " = false",
                isInline + " = false",
                isExpect + " = false",
                isFun,
                source,
                irFactory,
            ).join(separator = ",\n").indent(),
            +") : " + baseClasses("Class") + " " + blockSpaced(
                initBlock,
                commonFields,
                descriptor(ClassDescriptor),
                visibilityField.toPersistentField(+"visibility"),
                thisReceiverField.toPersistentField(+"null"),
                lines(
                    +"private var initialDeclarations: MutableList<" + IrDeclaration + ">? = null",
                    id,
                    +"override val declarations: MutableList<IrDeclaration> = " + import("ArrayList", "java.util") + "()",
                    lines(
                        +"get() " + block(
                            +"if (createdOn < factory.stageController.currentStage && initialDeclarations == null) " + block(
                                +"initialDeclarations = " + import("Collections", "java.util") + ".unmodifiableList(ArrayList(field))"
                            ),
                            id,
                            +"""
                                return if (factory.stageController.canAccessDeclarationsOf(this)) {
                                    ensureLowered()
                                    field
                                } else {
                                    initialDeclarations ?: field
                                }
                            """.trimIndent()
                        )
                    ).indent()
                ),
                typeParametersField.toPersistentField(+"emptyList()"),
                superTypesField.toPersistentField(+"emptyList()"),
                +"override var metadata: " + MetadataSource + "? = null",
                modalityField.toPersistentField(+"modality"),
                inlineClassRepresentationField.toPersistentField(+"null"),
                +"override var attributeOwnerId: " + IrAttributeContainer + " = this",
            ),
            id,
        )()
    })

    writeFile("carriers/ClassCarrier.kt", renderFile("org.jetbrains.kotlin.ir.declarations.persistent.carriers") {
        carriers(
            "Class",
            thisReceiverField,
            visibilityField,
            modalityField,
            typeParametersField,
            superTypesField,
            inlineClassRepresentationField,
        )()
    })

    addCarrierProtoMessage(
        "Class",
        thisReceiverField,
        visibilityField,
        modalityField,
        typeParametersField,
        superTypesField,
        inlineClassRepresentationField,
    )
}
