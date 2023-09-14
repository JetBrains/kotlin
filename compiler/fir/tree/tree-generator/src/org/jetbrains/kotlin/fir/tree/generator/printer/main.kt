/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.generators.tree.printer.GeneratedFile
import org.jetbrains.kotlin.generators.util.GeneratorsFileUtil.GENERATED_MESSAGE
import org.jetbrains.kotlin.utils.SmartPrinter
import java.io.File

const val VISITOR_PACKAGE = "org.jetbrains.kotlin.fir.visitors"
const val BASE_PACKAGE = "org.jetbrains.kotlin.fir"

internal const val TREE_GENERATOR_README = "compiler/fir/tree/tree-generator/Readme.md"

fun generateElements(builder: AbstractFirTreeBuilder, generationPath: File): List<GeneratedFile> {
    val generatedFiles = mutableListOf<GeneratedFile>()
    builder.elements.mapTo(generatedFiles) { it.generateCode(generationPath) }
    builder.elements.flatMap { it.allImplementations }.mapTo(generatedFiles) { it.generateCode(generationPath) }
    builder.elements.flatMap { it.allImplementations }.mapNotNull { it.builder }.mapTo(generatedFiles) { it.generateCode(generationPath) }
    builder.intermediateBuilders.mapTo(generatedFiles) { it.generateCode(generationPath) }

    generatedFiles += printVisitor(builder.elements, generationPath, false)
    generatedFiles += printVisitorVoid(builder.elements, generationPath)
    generatedFiles += printVisitor(builder.elements, generationPath, true)
    generatedFiles += printDefaultVisitorVoid(builder.elements, generationPath)
    generatedFiles += printTransformer(builder.elements, generationPath)
    return generatedFiles
}
