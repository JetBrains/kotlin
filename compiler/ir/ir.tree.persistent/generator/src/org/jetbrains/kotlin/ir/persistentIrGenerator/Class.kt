/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.persistentIrGenerator

internal fun PersistentIrGenerator.generateClass() {
    val visibilityField = Field("visibility", descriptorType("DescriptorVisibility"))
    val thisReceiverField = Field("thisReceiver", irDeclaration("IrValueParameter") + "?")
    val typeParametersField = Field("typeParameters", +"List<" + irDeclaration("IrTypeParameter") + ">")
    val superTypesField = Field("superTypes", +"List<" + import("IrType", "org.jetbrains.kotlin.ir.types") + ">")
    val metadataField = Field("metadata", irDeclaration("MetadataSource") + "?")
    val modalityField = Field("modality", descriptorType("Modality"))
    val attributeOwnerIdField = Field("attributeOwnerId", IrAttributeContainer)

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
                metadataField.toPersistentField(+"null"),
                modalityField.toPersistentField(+"modality"),
                attributeOwnerIdField.toPersistentField(+"this"),
            ),
            id,
        )()
    })

    writeFile("carriers/ClassCarrier.kt", renderFile("org.jetbrains.kotlin.ir.declarations.persistent.carriers") {
        carriers(
            "Class",
            thisReceiverField,
            metadataField,
            visibilityField,
            modalityField,
            attributeOwnerIdField,
            typeParametersField,
            superTypesField,
        )()
    })
}