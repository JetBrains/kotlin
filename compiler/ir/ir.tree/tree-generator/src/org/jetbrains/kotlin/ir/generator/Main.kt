/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator

import org.jetbrains.kotlin.generators.tree.imports.ArbitraryImportable
import org.jetbrains.kotlin.generators.tree.printer.TreeGenerator
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.print.*
import org.jetbrains.kotlin.ir.generator.print.symbol.*
import org.jetbrains.kotlin.utils.bind
import java.io.File

const val BASE_PACKAGE = "org.jetbrains.kotlin.ir"

internal const val TREE_GENERATOR_README = "compiler/ir/ir.tree/tree-generator/ReadMe.md"

typealias Model = org.jetbrains.kotlin.generators.tree.Model<Element>

fun main(args: Array<String>) {
    val generationPath = args.firstOrNull()?.let { File(it) }
        ?: File("compiler/ir/ir.tree/gen").canonicalFile

    val symbolModel = IrSymbolTree.build()
    val model = IrTree.build()
    TreeGenerator(generationPath, TREE_GENERATOR_README).run {
        generateTree(
            model,
            elementBaseType,
            ::ElementPrinter,
            listOf(
                elementVisitorType to ::VisitorPrinter,
                elementVisitorVoidType to ::VisitorVoidPrinter,
                elementTransformerType to ::TransformerPrinter.bind(model.rootElement),
                elementTransformerVoidType to ::TransformerVoidPrinter,
                typeTransformerType to ::TypeTransformerPrinter.bind(model.rootElement),
                typeTransformerVoidType to ::TypeTransformerVoidPrinter.bind(model.rootElement),
            ),
            ImplementationConfigurator,
            createImplementationPrinter = ::ImplementationPrinter,
            enableBaseTransformerTypeDetection = false,
            addFiles = {
                add(printFactory(generationPath, model))
                add(printSymbolRemapper(generationPath, model, declaredSymbolRemapperType, ::DeclaredSymbolRemapperInterfacePrinter))
                add(printSymbolRemapper(generationPath, model, referencedSymbolRemapperType, ::ReferencedSymbolRemapperInterfacePrinter))
                add(printSymbolRemapper(generationPath, model, symbolRemapperType, ::SymbolRemapperInterfacePrinter))
            }
        )

        generateTree(
            symbolModel,
            pureAbstractElement = null,
            ::SymbolPrinter.bind(model),
            createVisitorPrinters = emptyList(),
            SymbolImplementationConfigurator,
            createImplementationPrinter = ::SymbolImplementationPrinter,
            enableBaseTransformerTypeDetection = false,
            putElementsInSingleFile = Packages.symbols to "IrSymbol",
            putImplementationsInSingleFile = Packages.symbolsImpl to "IrSymbolImpl",
        )
    }
}
