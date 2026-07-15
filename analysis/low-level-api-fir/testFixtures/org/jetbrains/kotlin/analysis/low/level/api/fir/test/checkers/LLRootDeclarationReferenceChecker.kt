/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.checkers

import org.jetbrains.kotlin.analysis.low.level.api.fir.declarations.roots.checkRootDeclarationReferences
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices

/**
 * This checker ensures that every FIR declaration carries a correct back reference to its root declaration. See
 * [checkRootDeclarationReferences] for more information.
 *
 * There is no corresponding compiler checker because in compiler mode, FIR is trivially alive and no back references exist.
 */
class LLRootDeclarationReferenceChecker(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override fun check(thereWereFailures: Boolean) {
        // We ignore failed assertions. A missing or wrong back reference can lead to duplicate FIR once files are weakly referenced, so it
        // has a higher priority than the resolution test failure itself.

        checkAllFirFiles(testServices) { firFiles ->
            firFiles.forEach { checkRootDeclarationReferences(it) }
        }
    }
}
