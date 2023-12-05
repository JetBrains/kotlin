/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree.printer

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.config.AbstractBuilderConfigurator
import org.jetbrains.kotlin.generators.tree.config.AbstractImplementationConfigurator
import org.jetbrains.kotlin.generators.util.GeneratorsFileUtil
import org.jetbrains.kotlin.utils.SmartPrinter
import java.io.File

private val COPYRIGHT by lazy { File("license/COPYRIGHT_HEADER.txt").readText() }

class GeneratedFile(val file: File, val newText: String)

private fun getPathForFile(generationPath: File, packageName: String, typeName: String): File {
    val dir = generationPath.resolve(packageName.replace(".", "/"))
    return File(dir, "$typeName.kt")
}

fun printGeneratedType(
    generationPath: File,
    treeGeneratorReadMe: String,
    packageName: String,
    typeName: String,
    fileSuppressions: List<String> = emptyList(),
    body: context(ImportCollector) SmartPrinter.() -> Unit,
): GeneratedFile {
    val stringBuilder = StringBuilder()
    val file = getPathForFile(generationPath, packageName, typeName)
    val importCollector = ImportCollector(packageName)
    body(importCollector, SmartPrinter(stringBuilder))
    return GeneratedFile(
        file,
        buildString {
            appendLine(COPYRIGHT)
            appendLine()
            append("// This file was generated automatically. See ")
            append(treeGeneratorReadMe)
            appendLine(".")
            appendLine("// DO NOT MODIFY IT MANUALLY.")
            appendLine()
            if (fileSuppressions.isNotEmpty()) {
                fileSuppressions.joinTo(this, prefix = "@file:Suppress(", postfix = ")\n\n") { "\"$it\"" }
            }
            appendLine("package $packageName")
            appendLine()
            if (importCollector.printAllImports(this)) {
                appendLine()
            }
            append(stringBuilder)
        }
    )
}

/**
 * The entry point of the tree generator.
 *
 * @param generationPath Where to put the generated files.
 * @param treeGeneratorReadme A relative path to the README file of the tree generator. This path is mentioned in the auto-generation
 * warning in each generated file.
 * @param model The configured elements of the tree.
 * @param pureAbstractElement The abstract class for elements that only implement interfaces to inherit from, see [addPureAbstractElement]
 * for details.
 * @param createElementPrinter Provide the class that prints elements, see [AbstractElementPrinter].
 * @param createVisitorPrinters Provide the class that prints various visitors, see [AbstractVisitorPrinter].
 * @param implementationConfigurator The class for configuring the set of implementation classes, see [AbstractImplementation] and
 * [AbstractImplementationConfigurator].
 * @param builderConfigurator The class for configuring the set of builders, see [Builder] and [AbstractBuilderConfigurator].
 * @param createImplementationPrinter Provide the class that prints implementations of elements, see [AbstractImplementationPrinter].
 * @param createBuilderPrinter Provide the class that prints the corresponding builder for each element class, see [AbstractBuilderPrinter].
 * @param afterConfiguration The routine to run after all implementations and builders were fully configured,
 * but before anything was printed to a file.
 * @param enableBaseTransformerTypeDetection Whether to use a special algorithm for inferring return types of transformer methods for each
 * element, see [detectBaseTransformerTypes].
 * @param addFiles Arbitrary files to add to the set of generated files.
 */
fun <Element, Implementation, ElementField, ImplementationField> generateTree(
    generationPath: File,
    treeGeneratorReadme: String,
    model: Model<Element>,
    pureAbstractElement: ClassRef<*>,
    createElementPrinter: (SmartPrinter) -> AbstractElementPrinter<Element, ElementField>,
    createVisitorPrinters: List<Pair<ClassRef<*>, (SmartPrinter, ClassRef<*>) -> AbstractVisitorPrinter<Element, ElementField>>>,
    implementationConfigurator: AbstractImplementationConfigurator<Implementation, Element, ImplementationField>? = null,
    builderConfigurator: AbstractBuilderConfigurator<Element, Implementation, ImplementationField, ElementField>? = null,
    createImplementationPrinter: ((SmartPrinter) -> AbstractImplementationPrinter<Implementation, Element, ImplementationField>)? = null,
    createBuilderPrinter: ((SmartPrinter) -> AbstractBuilderPrinter<Element, Implementation, ImplementationField, ElementField>)? = null,
    afterConfiguration: () -> Unit = {},
    enableBaseTransformerTypeDetection: Boolean = true,
    addFiles: MutableList<GeneratedFile>.() -> Unit = {},
) where Element : AbstractElement<Element, ElementField, Implementation>,
        Implementation : AbstractImplementation<Implementation, Element, ImplementationField>,
        ElementField : AbstractField<ElementField>,
        ImplementationField : AbstractField<*>,
        ImplementationField : AbstractFieldWithDefaultValue<ElementField> {
    if (enableBaseTransformerTypeDetection) {
        detectBaseTransformerTypes(model)
    }
    implementationConfigurator?.configureImplementations(model)
    val implementations = model.elements.flatMap { it.allImplementations }
    InterfaceAndAbstractClassConfigurator((model.elements + implementations))
        .configureInterfacesAndAbstractClasses()
    addPureAbstractElement(model.elements, pureAbstractElement)
    builderConfigurator?.configureBuilders()
    afterConfiguration()
    val generatedFiles = mutableListOf<GeneratedFile>()

    model.elements.mapTo(generatedFiles) { element ->
        printGeneratedType(
            generationPath,
            treeGeneratorReadme,
            element.packageName,
            element.typeName,
        ) {
            createElementPrinter(this).printElement(element)
        }
    }

    if (createImplementationPrinter != null) {
        implementations.mapTo(generatedFiles) { implementation ->
            printGeneratedType(
                generationPath,
                treeGeneratorReadme,
                implementation.packageName,
                implementation.typeName,
                fileSuppressions = listOf("DuplicatedCode", "unused"),
            ) {
                createImplementationPrinter(this).printImplementation(implementation)
            }
        }
    }

    if (createBuilderPrinter != null && builderConfigurator != null) {
        (implementations.mapNotNull { it.builder } + builderConfigurator.intermediateBuilders).mapTo(generatedFiles) { builder ->
            printGeneratedType(
                generationPath,
                treeGeneratorReadme,
                builder.packageName,
                builder.typeName,
                fileSuppressions = listOf("DuplicatedCode", "unused"),
            ) {
                createBuilderPrinter(this).printBuilder(builder)
            }
        }
    }

    createVisitorPrinters.mapTo(generatedFiles) { (visitorClass, createVisitorPrinter) ->
        printGeneratedType(generationPath, treeGeneratorReadme, visitorClass.packageName, visitorClass.simpleName) {
            createVisitorPrinter(this, visitorClass).printVisitor(model.elements)
        }
    }

    generatedFiles.addFiles()

    val previouslyGeneratedFiles = GeneratorsFileUtil.collectPreviouslyGeneratedFiles(generationPath)
    generatedFiles.forEach { GeneratorsFileUtil.writeFileIfContentChanged(it.file, it.newText, logNotChanged = false) }
    GeneratorsFileUtil.removeExtraFilesFromPreviousGeneration(previouslyGeneratedFiles, generatedFiles.map { it.file })
}
