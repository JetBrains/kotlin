/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.persistentIrGenerator

internal fun PersistentIrGenerator.generateProperty() {
    val backingFieldField = Field(
        "backingField",
        IrField + "?",
        fieldProto,
        propSymbolType = IrFieldSymbol + "?",
        symbolToDeclaration = +"?.owner",
        declarationToSymbol = +"?.symbol"
    )
    val getterField = Field(
        "getter",
        IrSimpleFunction + "?",
        simpleFunctionProto,
        propSymbolType = IrSimpleFunctionSymbol + "?",
        symbolToDeclaration = +"?.owner",
        declarationToSymbol = +"?.symbol"
    )
    val setterField = Field(
        "setter",
        IrSimpleFunction + "?",
        simpleFunctionProto,
        propSymbolType = IrSimpleFunctionSymbol + "?",
        symbolToDeclaration = +"?.owner",
        declarationToSymbol = +"?.symbol"
    )
    val overriddenSymbolsField = Field(
        "overriddenSymbols",
        +"List<" + IrPropertySymbol + ">",
        propertySymbolListProto
    )

    writeFile("PersistentIrPropertyCommon.kt", renderFile("org.jetbrains.kotlin.ir.declarations.persistent") {
        lines(
            id,
            +"internal abstract class PersistentIrPropertyCommon(",
            arrayOf(
                startOffset,
                endOffset,
                origin,
                name,
                +"override var " + visibility, // TODO non-persisted state
                +"override val isVar: Boolean",
                +"override val isConst: Boolean",
                +"override val isLateinit: Boolean",
                +"override val isDelegated: Boolean",
                isExternal,
                isExpect,
                containerSource,
                irFactory,
            ).join(separator = ",\n").indent(),
            +") : " + baseClasses("Property") + " " + blockSpaced(
                commonFields,
                backingFieldField.toPersistentField(+"null"),
                getterField.toPersistentField(+"null"),
                setterField.toPersistentField(+"null"),
                overriddenSymbolsField.toPersistentField(+"emptyList()"),
                +"override var metadata: " + MetadataSource + "? = null",
                lines(
                    +"@Suppress(\"LeakingThis\")",
                    +"override var attributeOwnerId: " + IrAttributeContainer + " = this",
                ),
            ),
            id,
        )()
    })

    writeFile("carriers/PropertyCarrier.kt", renderFile("org.jetbrains.kotlin.ir.declarations.persistent.carriers") {
        carriers(
            "Property",
            backingFieldField,
            getterField,
            setterField,
            overriddenSymbolsField,
        )()
    })

    addCarrierProtoMessage(
        "Property",
        backingFieldField,
        getterField,
        setterField,
        overriddenSymbolsField,
    )
}