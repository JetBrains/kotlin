/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.klibSourceFileProvider

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.native.analysis.api.getSymbols
import org.jetbrains.kotlin.native.analysis.api.readKlibDeclarationAddresses
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktModuleProvider
import org.jetbrains.kotlin.analysis.test.framework.project.structure.mainModules
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import kotlin.test.fail

/**
 * Reads through the declarations provided in the .klib and renders their `klibSourceFile`
 */
abstract class AbstractGetKlibSourceFileNameTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByModuleStructure(moduleStructure: TestModuleStructure, testServices: TestServices) {
        val mainModule = testServices.ktModuleProvider.mainModules
            .let { modules -> if (modules.size == 1) modules.first() else fail("Expected single main module. Found $modules") }

        val libraryModule = mainModule.ktModule as? KtLibraryModule
            ?: fail("Expected main module '${mainModule.ktModule}' to be '${KtLibraryModule::class.simpleName}'")

        val actual = StringBuilder()
        actual.appendLine("klib declarations:")

        analyze(libraryModule) {
            val klibAddresses = libraryModule.readKlibDeclarationAddresses() ?: fail("Failed reading 'klib addresses' from $libraryModule")
            klibAddresses.forEach { klibDeclarationAddress ->
                klibDeclarationAddress.getSymbols().filterIsInstance<KtDeclarationSymbol>().forEach { symbol ->
                    val sourceFile = symbol.getKlibSourceFileName()
                    if (symbol is KtCallableSymbol) {
                        actual.appendLine("Callable: ${symbol.callableIdIfNonLocal}; klibSourceFile: $sourceFile")
                    }

                    if (symbol is KtClassOrObjectSymbol) {
                        actual.appendLine("Classifier: ${symbol.classIdIfNonLocal}; klibSourceFile: $sourceFile")
                    }
                }
            }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual.toString())
    }
}