/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.fir.PrivateForInline
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.typeOf

abstract class AbstractDiagnosticGroup @PrivateForInline constructor(val name: String, internal val containingObjectName: String) {
    @Suppress("PropertyName")
    @PrivateForInline
    val _diagnostics = mutableListOf<DiagnosticData>()

    @OptIn(PrivateForInline::class)
    val diagnostics: List<DiagnosticData>
        get() = _diagnostics

    @OptIn(PrivateForInline::class)
    internal inline fun <reified P : PsiElement> error(
        positioningStrategy: PositioningStrategy = PositioningStrategy.DEFAULT,
        crossinline init: DiagnosticBuilder.() -> Unit = {}
    ) = diagnosticDelegateProvider<P>(Severity.ERROR, positioningStrategy, init)


    @OptIn(PrivateForInline::class)
    internal inline fun <reified P : PsiElement> warning(
        positioningStrategy: PositioningStrategy = PositioningStrategy.DEFAULT,
        crossinline init: DiagnosticBuilder.() -> Unit = {}
    ) = diagnosticDelegateProvider<P>(Severity.WARNING, positioningStrategy, init)

    @PrivateForInline
    @OptIn(ExperimentalStdlibApi::class)
    internal inline fun <reified P : PsiElement> diagnosticDelegateProvider(
        severity: Severity,
        positioningStrategy: PositioningStrategy,
        crossinline init: DiagnosticBuilder.() -> Unit = {}
    ) = PropertyDelegateProvider<Any?, ReadOnlyProperty<AbstractDiagnosticGroup, DiagnosticData>> { _, property ->
        val diagnostic = DiagnosticBuilder(
            containingObjectName,
            severity,
            name = property.name,
            psiType = typeOf<P>(),
            positioningStrategy,
        ).apply(init).build()
        _diagnostics += diagnostic
        ReadOnlyProperty { _, _ -> diagnostic }
    }

    @OptIn(PrivateForInline::class)
    operator fun plus(other: AbstractDiagnosticGroup): AbstractDiagnosticGroup {
        require(name == other.name)
        return object : AbstractDiagnosticGroup(name, "#Stub") {
            init {
                _diagnostics.addAll(this.diagnostics)
                _diagnostics.addAll(other.diagnostics)
            }
        }
    }
}
