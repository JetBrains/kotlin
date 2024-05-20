/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import kotlin.reflect.KClass

public interface KaDiagnostic : KaLifetimeOwner {
    public val severity: Severity
    public val factoryName: String?
    public val defaultMessage: String
}

public typealias KtDiagnostic = KaDiagnostic

public interface KaDiagnosticWithPsi<out PSI : PsiElement> : KaDiagnostic {
    public val psi: PSI
    public val textRanges: Collection<TextRange>
    public val diagnosticClass: KClass<out KaDiagnosticWithPsi<PSI>>
}

public typealias KtDiagnosticWithPsi<PSI> = KaDiagnosticWithPsi<PSI>

public class KaNonBoundToPsiErrorDiagnostic(
    override val factoryName: String?,
    override val defaultMessage: String,
    override val token: KaLifetimeToken,
) : KaDiagnostic {
    override val severity: Severity get() = withValidityAssertion { Severity.ERROR }
}

public typealias KtNonBoundToPsiErrorDiagnostic = KaNonBoundToPsiErrorDiagnostic

public fun KaDiagnostic.getDefaultMessageWithFactoryName(): String =
    if (factoryName == null) defaultMessage
    else "[$factoryName] $defaultMessage"