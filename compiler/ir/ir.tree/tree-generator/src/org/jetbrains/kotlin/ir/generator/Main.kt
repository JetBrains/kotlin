/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.Model
import org.jetbrains.kotlin.generators.tree.printer.*
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.symbol.Symbol
import org.jetbrains.kotlin.ir.generator.model.symbol.SymbolField
import org.jetbrains.kotlin.ir.generator.model.symbol.SymbolImplementation
import org.jetbrains.kotlin.ir.generator.print.*
import org.jetbrains.kotlin.ir.generator.print.symbol.*
import org.jetbrains.kotlin.utils.bind
import java.io.File

const val BASE_PACKAGE = "org.jetbrains.kotlin.ir"

typealias Model = org.jetbrains.kotlin.generators.tree.Model<Element>

fun main(args: Array<String>) {
    val generationPath = args.firstOrNull()?.let { File(it) }
        ?: File("compiler/ir/ir.tree/gen").canonicalFile

    val model = IrTree.build()
    TreeGenerator(generationPath, "compiler/ir/ir.tree/tree-generator/ReadMe.md").run {
        printIrTree(model, generationPath)
        printIrSymbolTree(generationPath, model)
    }
}

private fun TreeGenerator.printIrTree(model: Model<Element>, generationPath: File) {
    model.inheritFields()
    model.specifyHasAcceptAndTransformChildrenMethods()

    ImplementationConfigurator.configureImplementations(model)
    val implementations = model.elements.flatMap { it.implementations }
    InterfaceAndAbstractClassConfigurator((model.elements + implementations))
        .configureInterfacesAndAbstractClasses()
    model.addPureAbstractElement(elementBaseType)

    printElements(model, ::ElementPrinter)
    printElementImplementations(implementations, ::ImplementationPrinter)
    printVisitors(
        model,
        listOf(
            irVisitorType to ::VisitorPrinter,
            irVisitorVoidType to ::VisitorVoidPrinter,
            irTransformerType to ::TransformerPrinter.bind(model.rootElement),
            elementTransformerVoidType to ::TransformerVoidPrinter,
            typeVisitorType to ::TypeVisitorPrinter.bind(model.rootElement),
            typeVisitorVoidType to ::TypeVisitorVoidPrinter.bind(model.rootElement),
            deepCopyIrTreeWithSymbolsType to ::DeepCopyIrTreeWithSymbolsPrinter,
            typeTransformerType to ::TypeTransformerPrinter.bind(model.rootElement),
            typeTransformerVoidType to ::TypeTransformerVoidPrinter.bind(model.rootElement),
        )
    )
}

private fun TreeGenerator.printIrSymbolTree(generationPath: File, model: Model<Element>) {
    val symbolModel = IrSymbolTree.build()
    symbolModel.inheritFields()

    SymbolImplementationConfigurator.configureImplementations(symbolModel)
    val implementations = symbolModel.elements.flatMap { it.implementations }
    InterfaceAndAbstractClassConfigurator((symbolModel.elements + implementations))
        .configureInterfacesAndAbstractClasses()
    model.addPureAbstractElement(elementBaseType)

    val elementsToPrint = symbolModel.elements.filter { it.doPrint }
    generatedFiles += printGeneratedTypesIntoSingleFile(
        elementsToPrint,
        generationPath,
        treeGeneratorReadme,
        Packages.symbols,
        "IrSymbol",
        makeTypePrinter = ::SymbolPrinter.bind(model),
        printType = AbstractElementPrinter<Symbol, SymbolField>::printElement,
    )

    val implementationsToPrint = implementations.filter { it.doPrint }
    generatedFiles += printGeneratedTypesIntoSingleFile(
        implementationsToPrint,
        generationPath,
        treeGeneratorReadme,
        Packages.symbolsImpl,
        "IrSymbolImpl",
        fileSuppressions = listOf("DuplicatedCode"),
        makeTypePrinter = ::SymbolImplementationPrinter,
        printType = AbstractImplementationPrinter<SymbolImplementation, Symbol, SymbolField>::printImplementation,
    )

    listOf(
        declaredSymbolRemapperType to ::DeclaredSymbolRemapperInterfacePrinter,
        referencedSymbolRemapperType to ::ReferencedSymbolRemapperInterfacePrinter,
        symbolRemapperType to ::SymbolRemapperInterfacePrinter,
    ).forEach { (type, makePrinter) ->
        generatedFiles += printGeneratedType(generationPath, treeGeneratorReadme, type.packageName, type.simpleName) {
            makePrinter(this, model.elements, type).printSymbolRemapper()
        }
    }
}
