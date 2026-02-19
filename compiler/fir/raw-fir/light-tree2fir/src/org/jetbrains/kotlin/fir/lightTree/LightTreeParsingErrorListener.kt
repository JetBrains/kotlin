/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree

import org.jetbrains.kotlin.KtOffsetsOnlySourceElement
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.builder.FirSyntaxErrors
import org.jetbrains.kotlin.parsing.KotlinLightParser

fun DiagnosticReporter.toKotlinParsingErrorListener(
    sourceFile: KtSourceFile,
    languageVersionSettings: LanguageVersionSettings
): KotlinLightParser.LightTreeParsingErrorListener {
    val diagnosticContext = object : DiagnosticContext {
        override val containingFilePath = sourceFile.path
        override val languageVersionSettings: LanguageVersionSettings get() = languageVersionSettings
        override fun isDiagnosticSuppressed(diagnostic: KtDiagnostic): Boolean = false
    }
    return KotlinLightParser.LightTreeParsingErrorListener { startOffset, endOffset, message ->
        reportOn(
            KtOffsetsOnlySourceElement(startOffset, endOffset),
            FirSyntaxErrors.SYNTAX,
            message.orEmpty(),
            diagnosticContext,
        )
    }
}
