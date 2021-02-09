/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator.diagnostics

import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.fir.PrivateForInline
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.typeOf

abstract class DiagnosticList {
    @Suppress("PropertyName")
    @PrivateForInline
    val _groups = mutableListOf<DiagnosticGroup>()

    @OptIn(PrivateForInline::class)
    val groups: List<DiagnosticGroup>
        get() = _groups

    val allDiagnostics: List<DiagnosticData>
        get() = groups.flatMap { it.diagnostics }


    @OptIn(PrivateForInline::class)
    operator fun DiagnosticGroup.provideDelegate(
        thisRef: DiagnosticList,
        prop: KProperty<*>
    ): ReadOnlyProperty<DiagnosticList, DiagnosticGroup> {
        val group = this
        _groups += group
        return ReadOnlyProperty { _, _ -> group }
    }
}

class DiagnosticBuilder(
    private val severity: Severity,
    private val name: String,
    private val sourceElementType: KType,
    private val psiType: KType,
    private val positioningStrategy: PositioningStrategy,
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
    fun build() = DiagnosticData(
        severity,
        name,
        sourceElementType,
        psiType,
        parameters,
        positioningStrategy,
    )
}