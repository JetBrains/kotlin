/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.test.directives.model.DirectivesContainer

abstract class MetaTestConfigurator(protected val testServices: TestServices) {
    open val directives: List<DirectivesContainer>
        get() = emptyList()

    open fun transformTestDataPath(testDataFileName: String): String = testDataFileName

    open fun shouldSkipTest(): Boolean = false
}
