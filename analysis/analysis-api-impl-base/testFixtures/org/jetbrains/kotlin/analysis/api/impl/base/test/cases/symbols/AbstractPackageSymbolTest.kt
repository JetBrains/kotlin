/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.configuration.AnalysisApiBinaryLibraryIndexingMode
import org.jetbrains.kotlin.analysis.test.framework.services.configuration.AnalysisApiIndexingConfiguration
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import kotlin.test.assertNotNull
import kotlin.test.assertNull

abstract class AbstractPackageSymbolTest : AbstractAnalysisApiBasedTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(Directives)

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.apply {
            // Library indexing is disabled in the test suite as otherwise the package will be returned from the indexed files.
            // The package provider, however, must work even with library indexing disabled.
            useAdditionalService { AnalysisApiIndexingConfiguration(AnalysisApiBinaryLibraryIndexingMode.NO_INDEXING) }
        }
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val allDirectives = testServices.moduleStructure.allDirectives

        analyze(mainFile) {
            for (packageName in allDirectives[Directives.HAS_PACKAGE]) {
                val packageSymbol = findPackage(packageName)
                assertNotNull(packageSymbol, "Package '$packageName' should exist")
            }

            for (packageName in allDirectives[Directives.NO_PACKAGE]) {
                val packageSymbol = findPackage(packageName)
                assertNull(packageSymbol, "Package '$packageName' should not exist")
            }
        }
    }

    private object Directives : SimpleDirectivesContainer() {
        val HAS_PACKAGE by valueDirective(
            description = "Check whether the specified package exists",
            parser = ::parsePackageName
        )

        val NO_PACKAGE by valueDirective(
            description = "Check whether the specified package does not exist",
            parser = ::parsePackageName
        )

        private fun parsePackageName(value: String): FqName {
            return if (value == "<root>") FqName.ROOT else FqName(value)
        }
    }
}