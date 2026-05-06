/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.checkDistinctSourceElements
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

/**
 * Checks that the source elements of FIR declarations in the analyzed FIR files are distinct. See [checkDistinctSourceElements] for more
 * information.
 *
 * The handler checks FIR files *after* analysis, which means that FIR elements have already been transformed. The tree might be different
 * compared to its raw state, so the checker might not catch all cases of duplicate source elements. To mitigate this, raw FIR files are
 * covered by `LightTreeDistinctSourceElementsTest` and `PsiDistinctSourceElementsTest`, which also check the diagnostic test data.
 *
 * See `LLDistinctSourceElementsChecker` for the corresponding Analysis API checker.
 */
class FirDistinctSourceElementsHandler(testServices: TestServices) : FirAnalysisHandler(testServices) {
    override fun processModule(module: TestModule, info: FirOutputArtifact) {
        for (part in info.partsForDependsOnModules) {
            val currentModule = part.module

            for (file in currentModule.files) {
                val firFile = info.mainFirFilesByTestFile[file] ?: continue

                // The test file's relative path is its logical file name in the test data. It isn't a path from the test data or project
                // root that can be necessarily found in the repository. However, because the error will be displayed for a specific failing
                // test, finding the affected test data is easy by following the failed test.
                checkDistinctSourceElements(listOf(firFile)) { _, _ -> "Duplicate source elements in test file '${file.relativePath}'" }
            }
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
    }
}
