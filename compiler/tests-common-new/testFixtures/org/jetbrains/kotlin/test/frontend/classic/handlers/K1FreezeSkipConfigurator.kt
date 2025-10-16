/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.classic.handlers

import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.IGNORE_K1_BECAUSE_IT_FREEZES
import org.jetbrains.kotlin.test.services.MetaTestConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

/**
 * Skip test for K1 in case it runs too slowly and basically freezes the IDE.
 */
class K1FreezeSkipConfigurator(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override fun shouldSkipTest(): Boolean = IGNORE_K1_BECAUSE_IT_FREEZES in testServices.moduleStructure.allDirectives
            && !testServices.moduleStructure.modules.first().languageVersionSettings.languageVersion.usesK2
}
