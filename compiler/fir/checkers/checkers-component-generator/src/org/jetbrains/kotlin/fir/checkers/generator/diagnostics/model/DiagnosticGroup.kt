/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.util.PrivateForInline
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
        crossinline init: DiagnosticBuilder.Regular.() -> Unit = {}
    ) = diagnosticDelegateProvider<P>(Severity.ERROR, positioningStrategy, init)


    @OptIn(PrivateForInline::class)
    internal inline fun <reified P : PsiElement> warning(
        positioningStrategy: PositioningStrategy = PositioningStrategy.DEFAULT,
        crossinline init: DiagnosticBuilder.Regular.() -> Unit = {}
    ) = diagnosticDelegateProvider<P>(Severity.WARNING, positioningStrategy, init)

    @OptIn(PrivateForInline::class)
    internal inline fun <reified P : PsiElement> deprecationError(
        featureForError: LanguageFeature,
        positioningStrategy: PositioningStrategy = PositioningStrategy.DEFAULT,
        crossinline init: DiagnosticBuilder.Deprecation.() -> Unit = {}
    ) = deprecationDiagnosticDelegateProvider<P>(featureForError, positioningStrategy, init)

    @PrivateForInline
    internal inline fun <reified P : PsiElement> diagnosticDelegateProvider(
        severity: Severity,
        positioningStrategy: PositioningStrategy,
        crossinline init: DiagnosticBuilder.Regular.() -> Unit = {}
    ) = PropertyDelegateProvider<Any?, ReadOnlyProperty<AbstractDiagnosticGroup, RegularDiagnosticData>> { _, property ->
        val diagnostic = DiagnosticBuilder.Regular(
            containingObjectName,
            severity,
            name = property.name,
            psiType = typeOf<P>(),
            positioningStrategy,
        ).apply(init).build()
        _diagnostics += diagnostic
        ReadOnlyProperty { _, _ -> diagnostic }
    }

    @PrivateForInline
    internal inline fun <reified P : PsiElement> deprecationDiagnosticDelegateProvider(
        featureForError: LanguageFeature,
        positioningStrategy: PositioningStrategy,
        crossinline init: DiagnosticBuilder.Deprecation.() -> Unit = {}
    ) = PropertyDelegateProvider<Any?, ReadOnlyProperty<AbstractDiagnosticGroup, DeprecationDiagnosticData>> { _, property ->
        val diagnostic = DiagnosticBuilder.Deprecation(
            containingObjectName,
            featureForError,
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

        val combinedDiagnostics = this.diagnostics + other.diagnostics

        return object : AbstractDiagnosticGroup(name, "#Stub") {
            init {
                _diagnostics.addAll(combinedDiagnostics)
            }
        }
    }
}
