/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestArtifactKind
import org.jetbrains.kotlin.test.model.TestFailureSuppressor
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.isLLFirTestData
import java.io.File

class FirFailingTestSuppressor(testServices: TestServices) : TestFailureSuppressor(testServices) {
    private val facadeKind: TestArtifactKind<*>
        get() = FrontendKinds.FIR

    override val order: Order
        get() = Order.P5

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        if (findFailFile() == null) return failedAssertions
        return failedAssertions.filterNot {
            when (it) {
                is WrappedException.FromFacade -> it.facade.outputKind == facadeKind
                is WrappedException.FromHandler -> it.handler.artifactKind == facadeKind
                is WrappedException.FromMetaInfoHandler -> true
                else -> false
            }
        }
    }

    override fun checkIfTestShouldBeUnmuted() {
        val failFile = findFailFile() ?: return

        // do not mute ll tests as they might behave differently
        if (testServices.moduleStructure.originalTestDataFiles.first().isLLFirTestData) return

        throw AssertionError("Fail file exists but no exceptions was thrown. Please remove ${failFile.name}")
    }

    private fun findFailFile(): File? {
        val testFile = testServices.moduleStructure.originalTestDataFiles.first()
        return testFile.parentFile.resolve("${testFile.nameWithoutExtension}.fail").takeIf { it.exists() }
    }
}
