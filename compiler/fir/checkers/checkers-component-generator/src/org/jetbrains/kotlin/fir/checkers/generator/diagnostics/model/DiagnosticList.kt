/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model

import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.fir.PrivateForInline
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.typeOf

abstract class DiagnosticList(internal val objectName: String) {
    @Suppress("PropertyName")
    @PrivateForInline
    val _groups = mutableListOf<AbstractDiagnosticGroup>()

    @OptIn(PrivateForInline::class)
    val groups: List<AbstractDiagnosticGroup>
        get() = _groups

    @OptIn(PrivateForInline::class)
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

    @OptIn(PrivateForInline::class)
    operator fun plus(other: DiagnosticList): DiagnosticList {
        val groupsByName = mutableMapOf<String, MutableList<AbstractDiagnosticGroup>>()

        fun collect(groups: List<AbstractDiagnosticGroup>) {
            for (group in groups) {
                val list = groupsByName.getOrPut(group.name) { mutableListOf() }
                list += group
            }
        }

        collect(groups)
        collect(other.groups)

        val resultingGroups = groupsByName.values.map {
            it.reduce { acc, group -> acc + group }
        }

        return object : DiagnosticList("#Stub") {
            init {
                _groups.addAll(resultingGroups)
            }
        }
    }

    @PrivateForInline
    abstract inner class DiagnosticGroup(name: String) : AbstractDiagnosticGroup(name, objectName)
}

class DiagnosticBuilder(
    private val containingObjectName: String,
    private val severity: Severity,
    private val name: String,
    private val psiType: KType,
    private val positioningStrategy: PositioningStrategy,
) {
    @PrivateForInline
    val parameters = mutableListOf<DiagnosticParameter>()

    @OptIn(PrivateForInline::class, ExperimentalStdlibApi::class)
    inline fun <reified T> parameter(name: String) {
        if (parameters.size >= MAX_DIAGNOSTIC_PARAMETER_COUNT) {
            error("Diagnostic cannot have more than $MAX_DIAGNOSTIC_PARAMETER_COUNT parameters")
        }
        parameters += DiagnosticParameter(
            name = name,
            type = typeOf<T>()
        )
    }

    @OptIn(PrivateForInline::class)
    fun build() = DiagnosticData(
        containingObjectName,
        severity,
        name,
        psiType,
        parameters,
        positioningStrategy,
    )

    companion object {
        const val MAX_DIAGNOSTIC_PARAMETER_COUNT = 4
    }
}
