/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.migration

import com.intellij.codeInspection.CleanupLocalInspectionTool
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.diagnostics.DiagnosticFactoryWithPsiElement
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.configuration.MigrationInfo
import org.jetbrains.kotlin.idea.configuration.isLanguageVersionUpdate
import org.jetbrains.kotlin.idea.quickfix.migration.MigrationFix
import org.jetbrains.kotlin.psi.KtAnnotationEntry


class ProhibitRepeatedUseSiteTargetAnnotationsMigrationInspection :
    AbstractDiagnosticBasedMigrationInspection<KtAnnotationEntry>(KtAnnotationEntry::class.java),
    MigrationFix,
    CleanupLocalInspectionTool {
    override fun isApplicable(migrationInfo: MigrationInfo): Boolean {
        return migrationInfo.isLanguageVersionUpdate(LanguageVersion.KOTLIN_1_3, LanguageVersion.KOTLIN_1_4)
    }

    override val diagnosticFactory: DiagnosticFactoryWithPsiElement<KtAnnotationEntry, *>
        get() = Errors.REPEATED_ANNOTATION
}

