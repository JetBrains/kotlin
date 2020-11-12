/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.persistentIrGenerator

internal fun PersistentIrGenerator.generateLocalDelegatedProperty() {
    val typeField = Field("type", IrType, typeProtoType)
    val delegateField = Field("delegate", irDeclaration("IrVariable"), variableProtoType, lateinit = true)
    val getterField = Field("getter", irDeclaration("IrFunction"), symbolProtoType, lateinit = true)
    val setterField = Field("setter", irDeclaration("IrFunction") + "?", symbolProtoType)

    writeFile("PersistentIrLocalDelegatedProperty.kt", renderFile("org.jetbrains.kotlin.ir.declarations.persistent") {
        lines(
            id,
            +"// TODO make not persistent",
            +"internal class PersistentIrLocalDelegatedProperty(",
            arrayOf(
                startOffset,
                endOffset,
                origin,
                +"override val symbol: " + irSymbol("IrLocalDelegatedPropertySymbol"),
                name,
                +"type: " + IrType,
                +"override val isVar: Boolean",
                irFactory,
            ).join(separator = ",\n").indent(),
            +") : " + baseClasses("LocalDelegatedProperty") + " " + blockSpaced(
                initBlock,
                commonFields,
                descriptor(descriptorType("VariableDescriptorWithAccessors")),
                typeField.toPersistentField(+"type"),
                delegateField.toPersistentField(+"null"),
                getterField.toPersistentField(+"null"),
                setterField.toPersistentField(+"null"),
                +"override var metadata: " + MetadataSource + "? = null",
            ),
            id,
        )()
    })

    writeFile("carriers/LocalDelegatedPropertyCarrier.kt", renderFile("org.jetbrains.kotlin.ir.declarations.persistent.carriers") {
        carriers(
            "LocalDelegatedProperty",
            typeField,
            delegateField,
            getterField,
            setterField,
        )()
    })

    addCarrierProtoMessage("LocalDelegatedProperty", typeField, delegateField, getterField, setterField)
}