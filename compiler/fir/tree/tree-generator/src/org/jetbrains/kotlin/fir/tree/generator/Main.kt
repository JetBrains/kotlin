/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator

import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.printer.generateElements
import org.jetbrains.kotlin.generators.tree.InterfaceAndAbstractClassConfigurator
import org.jetbrains.kotlin.generators.tree.Model
import org.jetbrains.kotlin.generators.tree.addPureAbstractElement
import org.jetbrains.kotlin.generators.tree.detectBaseTransformerTypes
import org.jetbrains.kotlin.generators.util.GeneratorsFileUtil
import org.jetbrains.kotlin.generators.util.GeneratorsFileUtil.collectPreviouslyGeneratedFiles
import org.jetbrains.kotlin.generators.util.GeneratorsFileUtil.removeExtraFilesFromPreviousGeneration
import java.io.File


fun main(args: Array<String>) {
    val generationPath = args.firstOrNull()?.let { File(it) }
        ?: File("../../tree/gen").canonicalFile

    NodeConfigurator.configureFields()
    val model = Model(FirTreeBuilder.elements, AbstractFirTreeBuilder.baseFirElement)
    detectBaseTransformerTypes(model)
    ImplementationConfigurator.configureImplementations(model)
    InterfaceAndAbstractClassConfigurator((model.elements + model.elements.flatMap { it.allImplementations }))
        .configureInterfacesAndAbstractClasses()
    addPureAbstractElement(FirTreeBuilder.elements, pureAbstractElementType)
    BuilderConfigurator.configureBuilders()
    val previouslyGeneratedFiles = collectPreviouslyGeneratedFiles(generationPath)
    val generatedFiles = generateElements(FirTreeBuilder, generationPath)
    generatedFiles.forEach { GeneratorsFileUtil.writeFileIfContentChanged(it.file, it.newText, logNotChanged = false) }
    removeExtraFilesFromPreviousGeneration(previouslyGeneratedFiles, generatedFiles.map { it.file })
}
