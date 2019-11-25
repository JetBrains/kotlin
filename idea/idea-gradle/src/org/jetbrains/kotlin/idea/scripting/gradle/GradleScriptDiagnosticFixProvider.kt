/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.kotlin.idea.actions.ShowKotlinGradleDslLogs
import org.jetbrains.kotlin.idea.script.ScriptDiagnosticFixProvider
import kotlin.script.experimental.api.ScriptDiagnostic

class GradleScriptDiagnosticFixProvider : ScriptDiagnosticFixProvider {
    override fun provideFixes(annotation: ScriptDiagnostic): List<IntentionAction> {
        if (gradleMessagesForQuickFix.any { annotation.message.contains(it) }) {
            return listOf(ShowKotlinGradleDslLogs())
        }
        return emptyList()
    }

    private val gradleMessagesForQuickFix = arrayListOf(
        "This script caused build configuration to fail",
        "see IDE logs for more information",
        "Script dependencies resolution failed"
    )
}