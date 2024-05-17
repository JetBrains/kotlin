/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references

import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleValue
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import kotlin.test.assertNotNull

abstract class AbstractReferenceImportAliasTest : AbstractAnalysisApiBasedTest() {
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            useDirectives(Directives)
        }
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val position = testServices.expressionMarkerProvider.getCaretPosition(mainFile)
        val reference = mainFile.findReferenceAt(position)
        assertNotNull(reference)
        val expectedAlias = mainModule.testModule.directives.singleValue(Directives.TYPE_ALIAS)
        val importDirective = mainFile.importDirectives.find { it.aliasName == expectedAlias }
        assertNotNull(importDirective)
        val importAlias = importDirective.alias
        assertNotNull(importAlias)
        testServices.assertions.assertTrue(reference.isReferenceTo(importAlias))
    }

    private object Directives : SimpleDirectivesContainer() {
        val TYPE_ALIAS by stringDirective(
            description = "Type alias for selected reference"
        )
    }
}