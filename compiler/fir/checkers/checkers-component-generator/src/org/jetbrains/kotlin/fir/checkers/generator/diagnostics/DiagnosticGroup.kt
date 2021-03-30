/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.PrivateForInline
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.typeOf

abstract class DiagnosticGroup @PrivateForInline constructor(val name: String) {
    @Suppress("PropertyName")
    @PrivateForInline
    val _diagnostics = mutableListOf<DiagnosticData>()

    @OptIn(PrivateForInline::class)
    val diagnostics: List<DiagnosticData>
        get() = _diagnostics

    @OptIn(PrivateForInline::class)
    inline fun <reified E : FirSourceElement, reified P : PsiElement> error(
        positioningStrategy: PositioningStrategy = PositioningStrategy.DEFAULT,
        crossinline init: DiagnosticBuilder.() -> Unit = {}
    ) = diagnosticDelegateProvider<E, P>(Severity.ERROR, positioningStrategy, init)


    @OptIn(PrivateForInline::class)
    inline fun <reified E : FirSourceElement, reified P : PsiElement> warning(
        positioningStrategy: PositioningStrategy = PositioningStrategy.DEFAULT,
        crossinline init: DiagnosticBuilder.() -> Unit = {}
    ) = diagnosticDelegateProvider<E, P>(Severity.WARNING, positioningStrategy, init)

    @PrivateForInline
    @OptIn(ExperimentalStdlibApi::class)
    inline fun <reified E : FirSourceElement, reified P : PsiElement> diagnosticDelegateProvider(
        severity: Severity,
        positioningStrategy: PositioningStrategy,
        crossinline init: DiagnosticBuilder.() -> Unit = {}
    ) = PropertyDelegateProvider<Any?, ReadOnlyProperty<DiagnosticGroup, DiagnosticData>> { _, property ->
        val diagnostic = DiagnosticBuilder(
            severity,
            name = property.name,
            sourceElementType = typeOf<E>(),
            psiType = typeOf<P>(),
            positioningStrategy,
        ).apply(init).build()
        _diagnostics += diagnostic
        ReadOnlyProperty { _, _ -> diagnostic }
    }
}