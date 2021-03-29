/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.util.*

class DiagnosticsService(val testServices: TestServices) : TestService {
    private val conditionsPerModule: MutableMap<TestModule, Condition<String>> = mutableMapOf()

    fun shouldRenderDiagnostic(module: TestModule, name: String): Boolean {
        val condition = conditionsPerModule.getOrPut(module) {
            computeDiagnosticConditionForModule(module)
        }
        return condition(name)
    }

    private fun computeDiagnosticConditionForModule(module: TestModule): Condition<String> {
        val diagnosticsInDirective = module.directives[DiagnosticsDirectives.DIAGNOSTICS]
        val enabledNames = mutableSetOf<String>()
        val disabledNames = mutableSetOf<String>()
        for (diagnosticInDirective in diagnosticsInDirective) {
            val enabled = when {
                diagnosticInDirective.startsWith("+") -> true
                diagnosticInDirective.startsWith("-") -> false
                else -> error("Incorrect diagnostics directive syntax. See reference:\n${DiagnosticsDirectives.DIAGNOSTICS.description}")
            }
            val name = diagnosticInDirective.substring(1)
            val collection = if (enabled) enabledNames else disabledNames
            collection += name
        }
        if (disabledNames.isEmpty()) return Conditions.alwaysTrue()
        var condition = !Conditions.oneOf(disabledNames)
        if (enabledNames.isNotEmpty()) {
            condition = condition or Conditions.oneOf(enabledNames)
        }
        return condition.cached()
    }

}

val TestServices.diagnosticsService: DiagnosticsService by TestServices.testServiceAccessor()
