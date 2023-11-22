/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator

import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.printer.*
import org.jetbrains.kotlin.generators.tree.Model
import org.jetbrains.kotlin.generators.tree.printer.generateTree
import org.jetbrains.kotlin.utils.bind
import java.io.File

internal const val BASE_PACKAGE = "org.jetbrains.kotlin.fir"
internal const val VISITOR_PACKAGE = "$BASE_PACKAGE.visitors"

fun main(args: Array<String>) {
    val generationPath = args.firstOrNull()?.let { File(it) }
        ?: File("../../tree/gen").canonicalFile
    NodeConfigurator.configureFields()
    val model = Model(FirTreeBuilder.elements, AbstractFirTreeBuilder.baseFirElement)
    generateTree(
        generationPath,
        "compiler/fir/tree/tree-generator/Readme.md",
        model,
        pureAbstractElementType,
        ::ElementPrinter,
        listOf(
            firVisitorType to ::VisitorPrinter.bind(false),
            firDefaultVisitorType to ::VisitorPrinter.bind(true),
            firVisitorVoidType to ::VisitorVoidPrinter,
            firDefaultVisitorVoidType to ::DefaultVisitorVoidPrinter,
            firTransformerType to ::TransformerPrinter.bind(model.rootElement),
        ),
        ImplementationConfigurator,
        BuilderConfigurator,
        ::ImplementationPrinter,
        ::BuilderPrinter,
    )
}
