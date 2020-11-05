/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.persistentIrGenerator

internal fun PersistentIrGenerator.generateAnonymousInitializer() {
    val body = Field("body", IrBlockBody, lateinit = true)

    writeFile("PersistentIrAnonymousInitializer.kt", renderFile("org.jetbrains.kotlin.ir.declarations.persistent") {
        lines(
            id,
            +"internal class PersistentIrAnonymousInitializer(",
            arrayOf(
                startOffset,
                endOffset,
                origin,
                +"override val symbol: " + IrAnonymousInitializerSymbol,
                isStatic + " = false",
                irFactory,
            ).join(separator = ",\n").indent(),
            +") : " + baseClasses("AnonymousInitializer") + " " + blockSpaced(
                initBlock,
                commonFields,
                descriptor(ClassDescriptor),
                body.toBody(),
            ),
            id,
        )()
    })

    writeFile("carriers/AnonymousInitializerCarrier.kt", renderFile("org.jetbrains.kotlin.ir.declarations.persistent.carriers") {
        carriers("AnonymousInitializer", body)()
    })
}