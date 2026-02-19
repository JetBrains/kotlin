/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator

import org.jetbrains.kotlin.fir.tree.generator.model.Element
import org.jetbrains.kotlin.fir.tree.generator.printer.*
import org.jetbrains.kotlin.generators.tree.InterfaceAndAbstractClassConfigurator
import org.jetbrains.kotlin.generators.tree.detectBaseTransformerTypes
import org.jetbrains.kotlin.generators.tree.printer.TreeGenerator
import org.jetbrains.kotlin.utils.bind
import java.io.File

internal const val BASE_PACKAGE = "org.jetbrains.kotlin.fir"
internal const val VISITOR_PACKAGE = "$BASE_PACKAGE.visitors"

typealias Model = org.jetbrains.kotlin.generators.tree.Model<Element>

fun main(args: Array<String>) {
    val generationPath = args.firstOrNull()?.let { File(it) }
        ?: File("../../tree/gen").canonicalFile

    val model = FirTree.build()
    TreeGenerator(generationPath, "compiler/fir/tree/tree-generator/Readme.md").run {
        model.inheritFields()
        detectBaseTransformerTypes(model)

        ImplementationConfigurator.configureImplementations(model)
        val implementations = model.elements.flatMap { it.implementations }
        InterfaceAndAbstractClassConfigurator((model.elements + implementations))
            .configureInterfacesAndAbstractClasses()
        model.addPureAbstractElement(pureAbstractElementType)

        val builderConfigurator = BuilderConfigurator(model)
        builderConfigurator.configureBuilders()

        printElements(model, ::ElementPrinter)
        printElementImplementations(implementations, ::ImplementationPrinter)
        printElementBuilders(implementations.mapNotNull { it.builder } + builderConfigurator.intermediateBuilders, ::BuilderPrinter)
        printVisitors(
            model,
            listOf(
                firVisitorType to ::VisitorPrinter.bind(false),
                firDefaultVisitorType to ::VisitorPrinter.bind(true),
                firVisitorVoidType to ::VisitorVoidPrinter,
                firDefaultVisitorVoidType to ::DefaultVisitorVoidPrinter,
                firTransformerType to ::TransformerPrinter.bind(model.rootElement),
            )
        )
    }
}
