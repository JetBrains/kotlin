/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.checkers

import org.jetbrains.kotlin.analysis.low.level.api.fir.symbols.id.checkSymbolIdConstraints
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices

/**
 * This checker ensures that the symbol ID constraints for non-unique FIR declarations from "FIR as Data" (KT-84343) hold after analysis.
 * See [checkSymbolIdConstraints] for more information.
 *
 * FIR files are checked in the state they were resolved to, or built fresh and checked as raw FIR if they haven't been resolved yet.
 *
 * There is no corresponding compiler checker because in compiler mode, FIR declarations universally have unique symbol IDs, so constraints
 * for non-unique FIR declarations do not apply.
 */
class LLSymbolIdConstraintsChecker(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override fun check(thereWereFailures: Boolean) {
        // We ignore failed assertions. With symbol IDs, constraint violations can easily lead to resolution problems, so they have a
        // higher priority than the resolution test failure itself.

        checkAllFirFiles(testServices) { firFiles ->
            checkSymbolIdConstraints(firFiles)
        }
    }
}
