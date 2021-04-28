/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.compiler.based

import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.removeDirectiveFromFile

class IdeTestIgnoreHandler(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    companion object {
        private val isTeamCityBuild: Boolean = System.getenv("TEAMCITY_VERSION") != null
    }

    override val directives: List<DirectivesContainer>
        get() = listOf(FirIdeDirectives)

    override fun check(failedAssertions: List<Throwable>) {
        if (!isFirIdeIgnoreDirectivePresent()) return
        if (failedAssertions.isNotEmpty()) return
        val moduleStructure = testServices.moduleStructure
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()

        if (!isTeamCityBuild) {
            testDataFile.removeDirectiveFromFile(FirIdeDirectives.FIR_IDE_IGNORE)
        }

        val message = if (isTeamCityBuild) {
            "Please remove // ${FirIdeDirectives.FIR_IDE_IGNORE} from the test source"
        } else {
            "Removed // ${FirIdeDirectives.FIR_IDE_IGNORE} from the test source"
        }
        throw TestWithFirIdeIgnoreAnnotationPassesException(
            """
                    Test pass in FIR IDE
                    $message
                    Please re-run the test now
                """.trimIndent()
        )
    }


    override fun suppressIfNeeded(failedAssertions: List<Throwable>): List<Throwable> {
        return if (isFirIdeIgnoreDirectivePresent())
            failedAssertions.filterIsInstance<TestWithFirIdeIgnoreAnnotationPassesException>()
        else failedAssertions
    }

    private fun isFirIdeIgnoreDirectivePresent(): Boolean {
        val moduleStructure = testServices.moduleStructure
        return FirIdeDirectives.FIR_IDE_IGNORE in moduleStructure.allDirectives
    }
}

class TestWithFirIdeIgnoreAnnotationPassesException(override val message: String) : IllegalStateException()