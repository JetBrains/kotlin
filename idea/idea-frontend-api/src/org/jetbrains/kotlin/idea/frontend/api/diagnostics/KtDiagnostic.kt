/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import kotlin.reflect.KClass

interface KtDiagnostic : ValidityTokenOwner {
    val severity: Severity
    val factoryName: String?
    val defaultMessage: String
}

interface KtDiagnosticWithPsi<out PSI: PsiElement> : KtDiagnostic {
    val psi: PSI
    val textRanges: Collection<TextRange>
    val diagnosticClass: KClass<out KtDiagnosticWithPsi<PSI>>
}

class KtNonBoundToPsiErrorDiagnostic(
    override val factoryName: String?,
    override val defaultMessage: String,
    override val token: ValidityToken,
) : KtDiagnostic {
    override val severity: Severity get() = Severity.ERROR
}

fun KtDiagnostic.getDefaultMessageWithFactoryName(): String =
    if (factoryName == null) defaultMessage
    else "[$factoryName] $defaultMessage"