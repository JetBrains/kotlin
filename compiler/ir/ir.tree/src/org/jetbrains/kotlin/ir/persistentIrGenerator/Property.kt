/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.persistentIrGenerator

internal fun PersistentIrGenerator.generateProperty() {
    val backingFieldField = Field("backingField", irDeclaration("IrField") + "?")
    val getterField = Field("getter", irDeclaration("IrSimpleFunction") + "?")
    val setterField = Field("setter", irDeclaration("IrSimpleFunction") + "?")
    val metadataField = Field("metadata", MetadataSource + "?")
    val attributeOwnerIdField = Field("attributeOwnerId", IrAttributeContainer)

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
                metadataField.toPersistentField(+"null"),
                lines(
                    +"@Suppress(\"LeakingThis\")",
                    attributeOwnerIdField.toPersistentField(+"this"),
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
            metadataField,
            attributeOwnerIdField,
        )()
    })
}