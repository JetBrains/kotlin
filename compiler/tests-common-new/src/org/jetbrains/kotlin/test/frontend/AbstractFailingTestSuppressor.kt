/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

abstract class AbstractFailingTestSuppressor(testServices: TestServices) : AfterAnalysisChecker(testServices) {

    protected abstract fun testFile(): File

    protected abstract fun hasFailure(failedAssertions: List<WrappedException>): Boolean

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        val failFile = testFile().parentFile.resolve("${testFile().nameWithoutExtension}.fail").takeIf { it.exists() }
            ?: return failedAssertions
        val failReason = failFile.readText().trim()
        if (hasFailure(failedAssertions) || failReason == INCONSISTENT_DIAGNOSTICS) return emptyList()
        return failedAssertions + AssertionError("Fail file exists but no exception was thrown. Please remove ${failFile.name}").wrap()
    }

    companion object {
        const val INCONSISTENT_DIAGNOSTICS = "INCONSISTENT_DIAGNOSTICS"
    }
}
