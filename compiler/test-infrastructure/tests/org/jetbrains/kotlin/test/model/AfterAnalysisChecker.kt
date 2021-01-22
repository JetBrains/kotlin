/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.model

import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices

abstract class AfterAnalysisChecker(protected val testServices: TestServices) {
    open val directives: List<DirectivesContainer>
        get() = emptyList()

    open fun check(failedAssertions: List<Throwable>) {}

    open fun suppressIfNeeded(failedAssertions: List<Throwable>): List<Throwable> = failedAssertions
}
