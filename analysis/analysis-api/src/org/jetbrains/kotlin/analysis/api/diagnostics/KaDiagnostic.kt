/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import kotlin.reflect.KClass

public enum class KaSeverity {
    ERROR,
    WARNING,
    INFO
}

public interface KaDiagnostic : KaLifetimeOwner {
    public val diagnosticClass: KClass<*>
    public val factoryName: String
    public val severity: KaSeverity

    public val defaultMessage: String
}

@Deprecated("Use 'KaDiagnostic' instead", ReplaceWith("KaDiagnostic"))
public typealias KtDiagnostic = KaDiagnostic

public interface KaDiagnosticWithPsi<out PSI : PsiElement> : KaDiagnostic {
    public override val diagnosticClass: KClass<out KaDiagnosticWithPsi<PSI>>

    public val psi: PSI
    public val textRanges: Collection<TextRange>
}

@Deprecated("Use 'KaDiagnosticWithPsi' instead", ReplaceWith("KaDiagnosticWithPsi<PSI>"))
public typealias KtDiagnosticWithPsi<PSI> = KaDiagnosticWithPsi<PSI>

public fun KaDiagnostic.getDefaultMessageWithFactoryName(): String {
    return "[$factoryName] $defaultMessage"
}