/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.callResolver

import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.callResolver.AbstractResolveTest.Directives.IGNORE_STABILITY
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.callResolver.AbstractResolveTest.Directives.IGNORE_STABILITY_K1
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.callResolver.AbstractResolveTest.Directives.IGNORE_STABILITY_K2
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.StringDirective

abstract class AbstractResolveTest : AbstractAnalysisApiBasedTest() {
    protected abstract val resolveKind: String

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.useDirectives(Directives)
    }

    private object Directives : SimpleDirectivesContainer() {
        val IGNORE_STABILITY by stringDirective(
            description = "Symbol restoring for some symbols in current test is not supported yet",
        )

        val IGNORE_STABILITY_K1 by stringDirective(
            description = "Symbol restoring for some symbols in current test is not supported yet in K1",
        )

        val IGNORE_STABILITY_K2 by stringDirective(
            description = "Symbol restoring for some symbols in current test is not supported yet in K2",
        )
    }

    protected fun RegisteredDirectives.doNotCheckSymbolRestoreDirective(): StringDirective? = findSpecificDirective(
        commonDirective = IGNORE_STABILITY,
        k1Directive = IGNORE_STABILITY_K1,
        k2Directive = IGNORE_STABILITY_K2,
    )

    protected fun ignoreStabilityIfNeeded(directives: RegisteredDirectives, body: () -> Unit) {
        val directive = directives.doNotCheckSymbolRestoreDirective()
        val isStabilitySuppressed = directive != null && directives[directive].let { values ->
            values.isEmpty() || values.any { it == resolveKind }
        }

        try {
            body()
        } catch (e: Throwable) {
            if (isStabilitySuppressed) return
            throw e
        }

        if (isStabilitySuppressed) {
            error("Directive '${directive.name}' is not needed")
        }
    }
}