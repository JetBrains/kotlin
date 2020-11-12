/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.persistentIrGenerator

internal fun PersistentIrGenerator.generateConstructor() {
    val returnTypeFieldField = Field("returnTypeField", IrType, "optional IrType")
    val typeParametersField = Field("typeParameters", +"List<" + IrTypeParameter + ">", "repeated IrTypeParameter")
    val dispatchReceiverParameterField = Field("dispatchReceiverParameter", IrValueParameter + "?", "optional IrValueParameter")
    val extensionReceiverParameterField = Field("extensionReceiverParameter", IrValueParameter + "?", "optional IrValueParameter")
    val valueParametersField = Field("valueParameters", +"List<" + IrValueParameter + ">", "repeated IrValueParameter")
    val bodyField = Field("body", IrBody + "?", "optional int32")
    val metadataField = Field("metadata", MetadataSource + "?")
    val visibilityField = Field("visibility", DescriptorVisibility)

    writeFile("PersistentIrConstructor.kt", renderFile("org.jetbrains.kotlin.ir.declarations.persistent") {
        lines(
            id,
            +"internal class PersistentIrConstructor(",
            arrayOf(
                startOffset,
                endOffset,
                origin,
                +"override val symbol: " + irSymbol("IrConstructorSymbol"),
                name,
                visibility,
                returnType,
                isInline,
                isExternal,
                isPrimary,
                isExpect,
                containerSource,
                irFactory,
            ).join(separator = ",\n").indent(),
            +") : " + baseClasses("Constructor") + " " + blockSpaced(
                initBlock,
                commonFields,
                returnTypeFieldField.toPersistentField(+"returnType", modifier = "private"),
                lines(
                    +"override var returnType: IrType",
                    lines(
                        +"get() = returnTypeField.let " + block(
                            +"if (it !== " + import("IrUninitializedType", "org.jetbrains.kotlin.ir.types.impl") + ") it else throw " + import("ReturnTypeIsNotInitializedException", "org.jetbrains.kotlin.ir.types.impl") + "(this)"
                        ),
                        +"set(c) " + block(
                            +"returnTypeField = c"
                        )
                    ).indent()
                ),
                typeParametersField.toPersistentField(+"emptyList()"),
                dispatchReceiverParameterField.toPersistentField(+"null"),
                extensionReceiverParameterField.toPersistentField(+"null"),
                valueParametersField.toPersistentField(+"emptyList()"),
                bodyField.toBody(),
                metadataField.toPersistentField(+"null"),
                visibilityField.toPersistentField(+"visibility"),
                descriptor(ClassConstructorDescriptor)
            ),
            id,
        )()
    })

    writeFile("carriers/ConstructorCarrier.kt", renderFile("org.jetbrains.kotlin.ir.declarations.persistent.carriers") {
        carriers(
            "Constructor",
            returnTypeFieldField,
            dispatchReceiverParameterField,
            extensionReceiverParameterField,
            bodyField,
            metadataField,
            visibilityField,
            typeParametersField,
            valueParametersField
        )()
    })

    addCarrierProtoMessage(
        "Constructor",
        returnTypeFieldField,
        dispatchReceiverParameterField,
        extensionReceiverParameterField,
        bodyField,
        typeParametersField,
        valueParametersField,
        withFlags = true
    )
}