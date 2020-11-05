/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.persistentIrGenerator

internal fun PersistentIrGenerator.generateValueParameter() {

    val defaultValueField = Field("defaultValue", IrExpressionBody + "?")
    val typeField = Field("type", IrType)
    val varargElementTypeField = Field("varargElementType", IrType + "?")

    writeFile("PersistentIrValueParameter.kt", renderFile("org.jetbrains.kotlin.ir.declarations.persistent") {
        lines(
            id,
            +"internal class PersistentIrValueParameter(",
            arrayOf(
                startOffset,
                endOffset,
                origin,
                +"override val symbol: " + irSymbol("IrValueParameterSymbol"),
                name,
                +"override val index: Int",
                +"type: " + IrType,
                +"varargElementType: " + IrType + "?",
                +"override val isCrossinline: Boolean",
                +"override val isNoinline: Boolean",
                +"override val isHidden: Boolean",
                +"override val isAssignable: Boolean",
                irFactory,
            ).join(separator = ",\n").indent(),
            +") : " + baseClasses("ValueParameter") + " " + blockSpaced(
                descriptor(descriptorType("ParameterDescriptor")),
                initBlock,
                commonFields,
                defaultValueField.toBody(),
                typeField.toPersistentField(+"type"),
                varargElementTypeField.toPersistentField(+"varargElementType"),
            ),
            id,
        )()
    })
    writeFile("carriers/ValueParameterCarrier.kt", renderFile("org.jetbrains.kotlin.ir.declarations.persistent.carriers") {
        carriers(
            "ValueParameter",
            defaultValueField,
            typeField,
            varargElementTypeField,
        )()
    })
}