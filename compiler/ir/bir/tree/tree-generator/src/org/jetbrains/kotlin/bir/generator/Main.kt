/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.generator

import org.jetbrains.kotlin.bir.generator.model.*
import org.jetbrains.kotlin.bir.generator.print.printBirMetadata
import org.jetbrains.kotlin.bir.generator.print.printElement
import org.jetbrains.kotlin.bir.generator.print.printElementImplementation
import org.jetbrains.kotlin.generators.tree.InterfaceAndAbstractClassConfigurator
import org.jetbrains.kotlin.generators.tree.addPureAbstractElement
import org.jetbrains.kotlin.generators.tree.detectBaseTransformerTypes
import org.jetbrains.kotlin.generators.tree.printer.TreeGenerator
import org.jetbrains.kotlin.generators.tree.printer.printGeneratedType
import java.io.File

const val BASE_PACKAGE = "org.jetbrains.kotlin.bir"

internal const val TREE_GENERATOR_README = "compiler/ir/bir.tree/tree-generator/ReadMe.md"

typealias Model = org.jetbrains.kotlin.generators.tree.Model<Element>

fun main(args: Array<String>) {
    val generationPath = args.firstOrNull()?.let { File(it) }
        ?: File("compiler/ir/bir/tree/gen").canonicalFile

    val symbolModel = BirSymbolTree.build()
    val model = BirTree.build()

    TreeGenerator(generationPath, TREE_GENERATOR_README).run {
        model.inheritFields()
        detectBaseTransformerTypes(model)
        InterfaceAndAbstractClassConfigurator(model.elements)
            .configureInterfacesAndAbstractClasses()
        addPureAbstractElement(model.elements, elementImplBaseType)
        model.setClassIds()
        model.computeFieldProperties()
        ImplementationConfigurator.configureImplementations(model)
        model.adjustSymbolOwners()
        val implementations = model.elements.flatMap { it.implementations }

        val elementsToPrint = model.elements.filter { it.doPrint }
        elementsToPrint.mapTo(generatedFiles) { element ->
            printGeneratedType(
                generationPath,
                TREE_GENERATOR_README,
                element.packageName,
                element.typeName,
            ) { printElement(element) }
        }

        val implementationsToPrint = implementations.filter { it.doPrint }
        implementationsToPrint.mapTo(generatedFiles) { implementation ->
            printGeneratedType(
                generationPath,
                TREE_GENERATOR_README,
                implementation.packageName,
                implementation.typeName,
                fileSuppressions = listOf("DuplicatedCode", "CanBePrimaryConstructorProperty"),
            ) { printElementImplementation(implementation) }
        }

        generatedFiles += printBirMetadata(generationPath, model)
    }
}
