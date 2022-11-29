/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.frontend.AbstractFailingTestSuppressor
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.firTestDataFile
import java.io.File

class FirFailingTestSuppressor(testServices: TestServices) : AbstractFailingTestSuppressor(testServices) {

    override fun testFile(): File {
        return testServices.moduleStructure.originalTestDataFiles.first().firTestDataFile
    }

    override fun hasFailure(failedAssertions: List<WrappedException>): Boolean {
        return failedAssertions.any {
            when (it) {
                is WrappedException.FromFacade -> it.facade is FirFrontendFacade
                is WrappedException.FromHandler -> it.handler.artifactKind == FrontendKinds.FIR
                else -> false
            }
        }
    }
}
