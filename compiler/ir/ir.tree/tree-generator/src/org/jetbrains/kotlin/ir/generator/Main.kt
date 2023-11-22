/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator

import org.jetbrains.kotlin.generators.tree.printer.generateTree
import org.jetbrains.kotlin.ir.generator.model.markLeaves
import org.jetbrains.kotlin.ir.generator.print.*
import org.jetbrains.kotlin.utils.bind
import java.io.File

const val BASE_PACKAGE = "org.jetbrains.kotlin.ir"

internal const val TREE_GENERATOR_README = "compiler/ir/ir.tree/tree-generator/ReadMe.md"

fun main(args: Array<String>) {
    val generationPath = args.firstOrNull()?.let { File(it) }
        ?: File("compiler/ir/ir.tree/gen").canonicalFile
    val model = IrTree.build()
    generateTree(
        generationPath,
        TREE_GENERATOR_README,
        model,
        elementBaseType,
        ::ElementPrinter,
        listOf(
            elementVisitorType to ::VisitorPrinter,
            elementVisitorVoidType to ::VisitorVoidPrinter,
            elementTransformerType to ::TransformerPrinter.bind(model.rootElement),
            elementTransformerVoidType to ::TransformerVoidPrinter,
            typeTransformerType to ::TypeTransformerPrinter.bind(model.rootElement),
        ),
        afterConfiguration = {
            markLeaves(model.elements)
        },
        enableBaseTransformerTypeDetection = false,
        addFiles = { add(printFactory(generationPath, model)) }
    )
}
