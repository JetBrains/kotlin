/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.model

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.services.TestServices

abstract class AfterAnalysisChecker(protected val testServices: TestServices) : ServicesAndDirectivesContainer {
    open fun check(failedAssertions: List<WrappedException>) {}

    open fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> = failedAssertions

    protected fun Throwable.wrap(): WrappedException = WrappedException.FromAfterAnalysisChecker(this)
}
