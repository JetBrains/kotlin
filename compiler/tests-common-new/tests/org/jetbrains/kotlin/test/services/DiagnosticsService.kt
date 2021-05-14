/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.util.*

class DiagnosticsService(val testServices: TestServices) : TestService {
    companion object {
        private val severityNameMapping = mapOf(
            "infos" to Severity.INFO,
            "warnings" to Severity.WARNING,
            "errors" to Severity.ERROR,
        )
    }

    private val conditionsPerModule: MutableMap<TestModule, Pair<Condition<String>, Condition<Severity>>> = mutableMapOf()

    fun shouldRenderDiagnostic(module: TestModule, name: String, severity: Severity): Boolean {
        val (nameCondition, severityCondition) = conditionsPerModule.getOrPut(module) {
            computeDiagnosticConditionForModule(module)
        }
        return severityCondition(severity) && nameCondition(name)
    }

    private fun computeDiagnosticConditionForModule(module: TestModule): Pair<Condition<String>, Condition<Severity>> {
        val diagnosticsInDirective = module.directives[DiagnosticsDirectives.DIAGNOSTICS]
        val enabledNames = mutableSetOf<String>()
        val disabledNames = mutableSetOf<String>()
        val enabledSeverities = mutableSetOf<Severity>()
        val disabledSeverities = mutableSetOf<Severity>()
        for (diagnosticInDirective in diagnosticsInDirective) {
            val enabled = when {
                diagnosticInDirective.startsWith("+") -> true
                diagnosticInDirective.startsWith("-") -> false
                else -> error("Incorrect diagnostics directive syntax. See reference:\n${DiagnosticsDirectives.DIAGNOSTICS.description}")
            }
            val name = diagnosticInDirective.substring(1)
            val severity = severityNameMapping[name]
            if (severity != null) {
                val collection = if (enabled) enabledSeverities else disabledSeverities
                collection += severity
            } else {
                val collection = if (enabled) enabledNames else disabledNames
                collection += name
            }
        }
        return computeCondition(enabledNames, disabledNames) to computeCondition(enabledSeverities, disabledSeverities)
    }

    private fun <T : Any> computeCondition(enabled: Set<T>, disabled: Set<T>): Condition<T> {
        if (disabled.isEmpty()) return Conditions.alwaysTrue()
        var condition = !Conditions.oneOf(disabled)
        if (enabled.isNotEmpty()) {
            condition = condition or Conditions.oneOf(enabled)
        }
        return condition.cached()
    }
}

val TestServices.diagnosticsService: DiagnosticsService by TestServices.testServiceAccessor()
