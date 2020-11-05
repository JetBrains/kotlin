/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.persistentIrGenerator

internal fun PersistentIrGenerator.generateTypeAlias() {
    val typeParametersField = Field("typeParameters", +"List<" + IrTypeParameter + ">")
    val expandedTypeField = Field("expandedType", IrType)

    writeFile("PersistentIrTypeAlias.kt", renderFile("org.jetbrains.kotlin.ir.declarations.persistent") {
        lines(
            id,
            +"internal class PersistentIrTypeAlias(",
            arrayOf(
                startOffset,
                endOffset,
                +"override val symbol: " + irSymbol("IrTypeAliasSymbol"),
                name,
                +"override var " + visibility,
                +"expandedType: " + IrType,
                +"override val isActual: Boolean",
                origin,
                irFactory,
            ).join(separator = ",\n").indent(),
            +") : " + baseClasses("TypeAlias") + " " + blockSpaced(
                initBlock,
                commonFields,
                descriptor(descriptorType("TypeAliasDescriptor")),

                typeParametersField.toPersistentField(+"emptyList()"),
                expandedTypeField.toPersistentField(+"expandedType"),
            ),
            id,
        )()
    })

    writeFile("carriers/TypeAliasCarrier.kt", renderFile("org.jetbrains.kotlin.ir.declarations.persistent.carriers") {
        carriers(
            "TypeAlias",
            typeParametersField,
            expandedTypeField,
        )()
    })
}