/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder

import java.io.File
import java.util.*

private val COPYRIGHT = """
/*
 * Copyright 2010-${GregorianCalendar()[Calendar.YEAR]} JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
""".trimIndent()

const val VISITOR_PACKAGE = "org.jetbrains.kotlin.fir.visitors"
const val BASE_PACKAGE = "org.jetbrains.kotlin.fir"
val GENERATED_MESSAGE = """
    /*
     * This file was generated automatically
     * DO NOT MODIFY IT MANUALLY
     */
     """.trimIndent()

fun printElements(builder: AbstractFirTreeBuilder, generationPath: File) {
    builder.elements.forEach { it.generateCode(generationPath) }
    builder.elements.flatMap { it.allImplementations }.forEach { it.generateCode(generationPath) }
    builder.elements.flatMap { it.allImplementations }.mapNotNull { it.builder }.forEach { it.generateCode(generationPath) }
    builder.intermediateBuilders.forEach { it.generateCode(generationPath) }

    printVisitor(builder.elements, generationPath)
    printVisitorVoid(builder.elements, generationPath)
    printTransformer(builder.elements, generationPath)
}

fun SmartPrinter.printCopyright() {
    println(COPYRIGHT)
    println()
}

fun SmartPrinter.printGeneratedMessage() {
    println(GENERATED_MESSAGE)
    println()
}
