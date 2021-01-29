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
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class DiagnosticListBuilder private constructor() {
    @PrivateForInline
    val diagnostics = mutableListOf<Diagnostic>()

    @PrivateForInline
    var currentGroupName: String? = null

    @OptIn(PrivateForInline::class)
    inline fun group(groupName: String, inner: () -> Unit) {
        if (currentGroupName != null) {
            error("Groups can not be nested ")
        }
        currentGroupName = groupName
        inner()
        currentGroupName = null
    }

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
    ) = PropertyDelegateProvider<Any?, AlwaysReturningUnitPropertyDelegate> { _, property ->
        diagnostics += DiagnosticBuilder(
            severity,
            name = property.name,
            sourceElementType = typeOf<E>(),
            psiType = typeOf<P>(),
            positioningStrategy,
            group = currentGroupName,
        ).apply(init).build()
        AlwaysReturningUnitPropertyDelegate
    }

    @PrivateForInline
    object AlwaysReturningUnitPropertyDelegate : ReadOnlyProperty<Any?, Unit> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) = Unit
    }

    @OptIn(PrivateForInline::class)
    private fun build() = DiagnosticList(diagnostics)

    companion object {
        fun buildDiagnosticList(init: DiagnosticListBuilder.() -> Unit) =
            DiagnosticListBuilder().apply(init).build()
    }
}

class DiagnosticBuilder(
    private val severity: Severity,
    private val name: String,
    private val sourceElementType: KType,
    private val psiType: KType,
    private val positioningStrategy: PositioningStrategy,
    private val group: String?
) {
    @PrivateForInline
    val parameters = mutableListOf<DiagnosticParameter>()

    @OptIn(PrivateForInline::class, ExperimentalStdlibApi::class)
    inline fun <reified T> parameter(name: String) {
        if (parameters.size == 3) {
            error("Diagnostic cannot have more than 3 parameters")
        }
        parameters += DiagnosticParameter(
            name = name,
            type = typeOf<T>()
        )
    }

    @OptIn(PrivateForInline::class)
    fun build() = Diagnostic(
        severity,
        name,
        sourceElementType,
        psiType,
        parameters,
        positioningStrategy,
        group
    )
}