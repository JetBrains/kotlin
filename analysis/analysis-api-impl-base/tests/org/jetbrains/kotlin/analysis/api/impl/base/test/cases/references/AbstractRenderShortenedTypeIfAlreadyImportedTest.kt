/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references

import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedSingleModuleTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * A class for reference shortener test.
 *
 * Note that it tests shortening only a single expression between <expr> and </expr> in the first file.
 */
abstract class AbstractRenderShortenedTypeIfAlreadyImportedTest : AbstractAnalysisApiBasedSingleModuleTest() {
    override fun doTestByFileStructure(ktFiles: List<KtFile>, module: TestModule, testServices: TestServices) {
        val container = testServices.expressionMarkerProvider.getSelectedElementOrNull(ktFiles.first())?.getNonStrictParentOfType<KtClass>()
            ?: ktFiles.first()

        val directives = module.directives
        val typesToShortenAsString = directives[Directives.TYPE]
        val renderedTypesAsString = executeOnPooledThreadInReadAction {
            analyseForTest(container) {
                typesToShortenAsString.joinToString(separator = "\n") {
                    val packageAndRelativeClass = it.split('/')
                    check(packageAndRelativeClass.size == 2) { "Malformed ClassId: $it -> it must be \"<package FqName>/<class FqName>\"" }

                    val packageFqname = FqName(packageAndRelativeClass.first())
                    val classFqName = FqName(packageAndRelativeClass.last())
                    val classId = ClassId(packageFqname, classFqName, isLocal = false)
                    renderShortenedTypeIfAlreadyImported(classId, container)
                }
            }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(renderedTypesAsString)
    }

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.useDirectives(Directives)
    }

    private object Directives : SimpleDirectivesContainer() {
        val TYPE by stringDirective(description = "Type to shorten if already imported")
    }
}