/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.model.TestArtifactKind
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

abstract class AbstractFailingFacadeSuppressor(testServices: TestServices) : AfterAnalysisChecker(testServices) {

    protected abstract fun testFile(): File

    protected abstract val facadeKind: TestArtifactKind<*>

    override val order: Order
        get() = Order.P5

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        val failFile = testFile().parentFile.resolve("${testFile().nameWithoutExtension}.fail").takeIf { it.exists() }
            ?: return failedAssertions
        val (suppressible, notSuppressible) = failedAssertions.partition {
            when (it) {
                is WrappedException.FromFacade -> it.facade.outputKind == facadeKind
                is WrappedException.FromHandler -> it.handler.artifactKind == facadeKind
                is WrappedException.FromMetaInfoHandler -> true
                else -> false
            }
        }

        return when {
            suppressible.isNotEmpty() -> notSuppressible
            else -> failedAssertions + AssertionError("Fail file exists but no exceptions was thrown. Please remove ${failFile.name}").wrap()
        }
    }
}
