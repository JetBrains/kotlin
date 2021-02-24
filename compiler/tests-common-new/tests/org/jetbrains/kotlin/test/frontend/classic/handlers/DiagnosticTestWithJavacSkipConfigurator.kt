/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.classic.handlers

import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.services.MetaTestConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

class DiagnosticTestWithJavacSkipConfigurator(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override val directives: List<DirectivesContainer>
        get() = listOf(DiagnosticsDirectives)

    override fun shouldSkipTest(): Boolean {
        return DiagnosticsDirectives.SKIP_JAVAC in testServices.moduleStructure.allDirectives
    }
}
