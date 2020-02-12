/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.migration

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.CleanupLocalInspectionTool
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactoryWithPsiElement
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.idea.configuration.MigrationInfo
import org.jetbrains.kotlin.idea.configuration.isLanguageVersionUpdate
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.quickfix.RemoveUnusedFunctionParameterFix
import org.jetbrains.kotlin.idea.quickfix.migration.MigrationFix
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter


class WarningOnMainUnusedParameterMigrationInspection :
    AbstractDiagnosticBasedMigrationInspection<KtParameter>(KtParameter::class.java),
    MigrationFix,
    CleanupLocalInspectionTool {
    override fun isApplicable(migrationInfo: MigrationInfo): Boolean {
        return migrationInfo.isLanguageVersionUpdate(LanguageVersion.KOTLIN_1_3, LanguageVersion.KOTLIN_1_4)
    }

    override val diagnosticFactory: DiagnosticFactoryWithPsiElement<KtParameter, *>
        get() = Errors.UNUSED_PARAMETER

    override fun getCustomIntentionFactory(): ((Diagnostic) -> IntentionAction?)? = fun(diagnostic: Diagnostic): IntentionAction? {
        val parameter = diagnostic.psiElement as? KtParameter ?: return null
        val ownerFunction = parameter.ownerFunction as? KtNamedFunction ?: return null
        val mainFunctionDetector = MainFunctionDetector(parameter.languageVersionSettings) { it.descriptor as? FunctionDescriptor }
        if (!mainFunctionDetector.isMain(ownerFunction)) return null
        return RemoveUnusedFunctionParameterFix(parameter, false)
    }
}
