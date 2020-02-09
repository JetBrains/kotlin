/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator

import org.jetbrains.kotlin.fir.tree.generator.printer.printElements
import org.jetbrains.kotlin.fir.tree.generator.util.*
import java.io.File


fun main(args: Array<String>) {
    val generationPath = args.firstOrNull()?.let { File(it) }
        ?: File("compiler/fir/tree/gen").absoluteFile

    NodeConfigurator.configureFields()
    detectBaseTransformerTypes(FirTreeBuilder)
    ImplementationConfigurator.configureImplementations()
    configureInterfacesAndAbstractClasses(FirTreeBuilder)
    BuilderConfigurator.configureBuilders()
    removePreviousGeneratedFiles(generationPath)
    printElements(FirTreeBuilder, generationPath)
//    printFieldUsageTable(FirTreeBuilder)
//    printHierarchyGraph(FirTreeBuilder)
}
