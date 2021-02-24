/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import java.io.File

private val COPYRIGHT = File("license/COPYRIGHT_HEADER.txt").readText()

const val VISITOR_PACKAGE = "org.jetbrains.kotlin.fir.visitors"
const val BASE_PACKAGE = "org.jetbrains.kotlin.fir"
val GENERATED_MESSAGE = """
    /*
     * This file was generated automatically
     * DO NOT MODIFY IT MANUALLY
     */
     """.trimIndent()

fun generateElements(builder: AbstractFirTreeBuilder, generationPath: File): List<GeneratedFile> {
    val generatedFiles = mutableListOf<GeneratedFile>()
    builder.elements.mapTo(generatedFiles) { it.generateCode(generationPath) }
    builder.elements.flatMap { it.allImplementations }.mapTo(generatedFiles) { it.generateCode(generationPath) }
    builder.elements.flatMap { it.allImplementations }.mapNotNull { it.builder }.mapTo(generatedFiles) { it.generateCode(generationPath) }
    builder.intermediateBuilders.mapTo(generatedFiles) { it.generateCode(generationPath) }

    generatedFiles += printVisitor(builder.elements, generationPath)
    generatedFiles += printVisitorVoid(builder.elements, generationPath)
    generatedFiles += printTransformer(builder.elements, generationPath)
    return generatedFiles
}

fun SmartPrinter.printCopyright() {
    println(COPYRIGHT)
    println()
}

fun SmartPrinter.printGeneratedMessage() {
    println(GENERATED_MESSAGE)
    println()
}
