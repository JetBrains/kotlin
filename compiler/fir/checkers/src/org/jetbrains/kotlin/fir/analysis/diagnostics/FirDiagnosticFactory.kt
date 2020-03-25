/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.fir.FirLightSourceElement
import org.jetbrains.kotlin.fir.FirPsiSourceElement
import org.jetbrains.kotlin.fir.FirSourceElement

sealed class AbstractFirDiagnosticFactory<E : FirSourceElement, D : FirDiagnostic<E>>(
    val name: String,
    val severity: Severity,
) {
    abstract val psiDiagnosticFactory: DiagnosticFactoryWithPsiElement<*, *>

    override fun toString(): String {
        return name
    }
}

class FirDiagnosticFactory0<E : FirSourceElement>(
    name: String, severity: Severity, override val psiDiagnosticFactory: DiagnosticFactory0<PsiElement>
) : AbstractFirDiagnosticFactory<E, FirSimpleDiagnostic<E>>(name, severity) {
    fun on(element: E): FirSimpleDiagnostic<E> {
        return when (element) {
            is FirPsiSourceElement -> FirPsiSimpleDiagnostic(element, severity, this)
            is FirLightSourceElement -> FirLightSimpleDiagnostic(element, severity, this)
            else -> incorrectElement(element)
        }
    }
}

class FirDiagnosticFactory1<E : FirSourceElement, A>(
    name: String, severity: Severity, override val psiDiagnosticFactory: DiagnosticFactory1<PsiElement, A>
) : AbstractFirDiagnosticFactory<E, FirDiagnosticWithParameters1<E, A>>(name, severity) {
    fun on(element: E, a: A): FirDiagnosticWithParameters1<E, A> {
        return when (element) {
            is FirPsiSourceElement -> FirPsiDiagnosticWithParameters1(element, a, severity, this)
            is FirLightSourceElement -> FirLightDiagnosticWithParameters1(element, a, severity, this)
            else -> incorrectElement(element)
        }
    }
}

class FirDiagnosticFactory2<E : FirSourceElement, A, B>(
    name: String, severity: Severity, override val psiDiagnosticFactory: DiagnosticFactory2<PsiElement, A, B>
) : AbstractFirDiagnosticFactory<E, FirDiagnosticWithParameters2<E, A, B>>(name, severity) {
    fun on(element: E, a: A, b: B): FirDiagnosticWithParameters2<E, A, B> {
        return when (element) {
            is FirPsiSourceElement -> FirPsiDiagnosticWithParameters2(element, a, b, severity, this)
            is FirLightSourceElement -> FirLightDiagnosticWithParameters2(element, a, b, severity, this)
            else -> incorrectElement(element)
        }
    }
}

class FirDiagnosticFactory3<E : FirSourceElement, A, B, C>(
    name: String, severity: Severity, override val psiDiagnosticFactory: DiagnosticFactory3<PsiElement, A, B, C>
) : AbstractFirDiagnosticFactory<E, FirDiagnosticWithParameters3<E, A, B, C>>(name, severity) {
    fun on(element: E, a: A, b: B, c: C): FirDiagnosticWithParameters3<E, A, B, C> {
        return when (element) {
            is FirPsiSourceElement -> FirPsiDiagnosticWithParameters3(element, a, b, c, severity, this)
            is FirLightSourceElement -> FirLightDiagnosticWithParameters3(element, a, b, c, severity, this)
            else -> incorrectElement(element)
        }
    }
}

private fun incorrectElement(element: FirSourceElement): Nothing {
    throw IllegalArgumentException("Unknown element type: ${element::class}")
}
