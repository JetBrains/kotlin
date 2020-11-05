/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.persistentIrGenerator

internal fun PersistentIrGenerator.generateTypeParameter() {
    val superTypesField = Field("superTypes", +"List<" + IrType + ">")

    writeFile("PersistentIrTypeParameter.kt", renderFile("org.jetbrains.kotlin.ir.declarations.persistent") {
        lines(
            id,
            +"internal class PersistentIrTypeParameter(",
            arrayOf(
                startOffset,
                endOffset,
                origin,
                +"override val symbol: " + irSymbol("IrTypeParameterSymbol"),
                name,
                +"override val index: Int",
                +"override val isReified: Boolean",
                +"override val variance: " + import("Variance", "org.jetbrains.kotlin.types"),
                irFactory,
            ).join(separator = ",\n").indent(),
            +") : " + baseClasses("TypeParameter") + " " + blockSpaced(
                initBlock,
                commonFields,
                descriptor(descriptorType("TypeParameterDescriptor")),
                superTypesField.toPersistentField(+"emptyList()"),
            ),
            id,
        )()
    })

    writeFile("carriers/TypeParameterCarrier.kt", renderFile("org.jetbrains.kotlin.ir.declarations.persistent.carriers") {
        carriers(
            "TypeParameter",
            superTypesField,
        )()
    })
}