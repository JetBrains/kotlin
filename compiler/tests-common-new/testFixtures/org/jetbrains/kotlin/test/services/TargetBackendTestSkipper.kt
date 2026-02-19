/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.DONT_TARGET_EXACT_BACKEND
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.TARGET_BACKEND

/**
 * Skips execution of the current test if its target backend doesn't satisfy backend
 * conditions specified with [TARGET_BACKEND] and [DONT_TARGET_EXACT_BACKEND] directives
 */
class TargetBackendTestSkipper(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override fun shouldSkipTest(): Boolean {
        val targetBackendOfTest = testServices.defaultsProvider.targetBackend ?: return false
        val directives = testServices.moduleStructure.allDirectives
        return !InTextDirectivesUtils.isCompatibleTarget(
            /* targetBackend = */ targetBackendOfTest,
            /* backends = */ directives[TARGET_BACKEND],
            /* doNotTarget = */ directives[DONT_TARGET_EXACT_BACKEND],
        )
    }
}
