/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.persistentIrGenerator

internal fun PersistentIrGenerator.generateLocalDelegatedProperty() {
    val typeField = Field("type", IrType)
    val delegateField = Field("delegate", irDeclaration("IrVariable"), lateinit = true)
    val getterField = Field("getter", irDeclaration("IrSimpleFunction"), lateinit = true)
    val setterField = Field("setter", irDeclaration("IrSimpleFunction") + "?")
    val metadataField = Field("metadata", MetadataSource + "?")

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
                metadataField.toPersistentField(+"null"),
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
            metadataField,
        )()
    })
}