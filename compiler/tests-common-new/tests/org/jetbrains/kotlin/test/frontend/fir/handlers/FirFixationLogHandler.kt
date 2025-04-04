/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.resolve.inference.inferenceComponents
import org.jetbrains.kotlin.resolve.calls.inference.components.VariableFixationFinder.FixationLogRecord
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.withExtension

class FirFixationLogHandler(testServices: TestServices) : FirAnalysisHandler(testServices) {

    val fixationLogs = mutableListOf<FixationLogRecord>()

    override fun processModule(
        module: TestModule,
        info: FirOutputArtifact,
    ) {
        if (!module.languageVersionSettings.supportsFeature(LanguageFeature.FixationLogs)) return
        for (part in info.partsForDependsOnModules) {
            fixationLogs += part.session.inferenceComponents.variableFixationFinder.fixationLogs
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val expectedFile = testServices.moduleStructure.originalTestDataFiles.first().withExtension(".fixation.txt")

        if (fixationLogs.isEmpty()) {
            assertions.assertFileDoesntExist(expectedFile) {
                "Fixation log file detected but no 'LANGUAGE: +FixationLogs' directive specified or no fixation was in fact performed."
            }
        } else {
            val actualText = buildString {
                for (log in fixationLogs) {
                    append(log)
                }
            }
            assertions.assertEqualsToFile(expectedFile, actualText, message = { "Content is not equal" })
        }
    }
}
