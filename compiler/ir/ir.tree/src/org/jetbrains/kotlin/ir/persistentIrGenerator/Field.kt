/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.persistentIrGenerator

internal fun PersistentIrGenerator.generateField() {
    val initializerField = Field("initializer", IrExpressionBody + "?", expressionBodyProto)
    val correspondingPropertySymbolField = Field("correspondingPropertySymbol", IrPropertySymbol + "?", propertySymbolProto)
    val typeField = Field("type", IrType, typeProto)

    writeFile("PersistentIrField.kt", renderFile("org.jetbrains.kotlin.ir.declarations.persistent") {
        lines(
            id,
            +"internal class PersistentIrField(",
            arrayOf(
                startOffset,
                endOffset,
                origin,
                +"override val symbol: " + irSymbol("IrFieldSymbol"),
                name,
                +"type: " + IrType,
                +"override var " + visibility,
                isFinal,
                isExternal,
                isStatic,
                irFactory,
            ).join(separator = ",\n").indent(),
            +") : " + baseClasses("Field") + " " + blockSpaced(
                initBlock,
                commonFields,
                descriptor(descriptorType("PropertyDescriptor")),
                initializerField.toBody(),
                correspondingPropertySymbolField.toPersistentField(+"null"),
                +"override var metadata: " + MetadataSource + "? = null",
                typeField.toPersistentField(+"type"),
            ),
            id,
        )()
    })

    writeFile("carriers/FieldCarrier.kt", renderFile("org.jetbrains.kotlin.ir.declarations.persistent.carriers") {
        carriers(
            "Field",
            typeField,
            initializerField,
            correspondingPropertySymbolField,
        )()
    })

    addCarrierProtoMessage("Field", typeField, initializerField, correspondingPropertySymbolField)
}