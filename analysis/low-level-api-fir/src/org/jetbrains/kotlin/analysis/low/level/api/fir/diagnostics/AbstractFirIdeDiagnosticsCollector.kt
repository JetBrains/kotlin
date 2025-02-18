/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.llFirModuleData
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.CheckersComponentInternal
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.ComposedDeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckersDiagnosticComponent
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ComposedExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckersDiagnosticComponent
import org.jetbrains.kotlin.fir.analysis.checkers.type.ComposedTypeCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.type.TypeCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.type.TypeCheckersDiagnosticComponent
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.analysis.collectors.DiagnosticCollectorComponents
import org.jetbrains.kotlin.fir.analysis.collectors.components.*
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.analysis.extensions.additionalCheckers
import org.jetbrains.kotlin.fir.analysis.js.checkers.JsDeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.js.checkers.JsExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.JvmDeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.JvmExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.JvmTypeCheckers
import org.jetbrains.kotlin.fir.analysis.native.checkers.NativeDeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.native.checkers.NativeExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.native.checkers.NativeTypeCheckers
import org.jetbrains.kotlin.fir.analysis.wasm.checkers.*
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.isWasm
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative

abstract class AbstractLLFirDiagnosticsCollector(
    session: FirSession,
    filter: DiagnosticCheckerFilter,
) : AbstractDiagnosticCollector(
    session,
    createComponents = { reporter ->
        CheckersFactory.createComponents(session, reporter, filter)
    }
)


object CheckersFactory {
    fun createComponents(
        session: FirSession,
        reporter: DiagnosticReporter,
        filter: DiagnosticCheckerFilter,
    ): DiagnosticCollectorComponents {
        val module = session.llFirModuleData.ktModule
        val platform = module.targetPlatform
        val extensionCheckers = session.extensionService.additionalCheckers
        val declarationCheckers = createDeclarationCheckers(filter, platform, extensionCheckers)
        val expressionCheckers = createExpressionCheckers(filter, platform, extensionCheckers)
        val typeCheckers = createTypeCheckers(filter, platform, extensionCheckers)

        val regularComponents = buildList {
            if (!filter.runExtraCheckers && !filter.runExperimentalCheckers) {
                add(ErrorNodeDiagnosticCollectorComponent(session, reporter))
            }
            add(DeclarationCheckersDiagnosticComponent(session, reporter, declarationCheckers))
            add(ExpressionCheckersDiagnosticComponent(session, reporter, expressionCheckers))
            add(TypeCheckersDiagnosticComponent(session, reporter, typeCheckers))
            add(ControlFlowAnalysisDiagnosticComponent(session, reporter, declarationCheckers))
        }.toTypedArray()
        return DiagnosticCollectorComponents(regularComponents, ReportCommitterDiagnosticComponent(session, reporter))
    }


    fun createDeclarationCheckers(
        filter: DiagnosticCheckerFilter,
        platform: TargetPlatform,
        extensionCheckers: List<FirAdditionalCheckersExtension>
    ) = createDeclarationCheckers {
        if (filter.runDefaultCheckers) {
            add(CommonDeclarationCheckers)
            add(CommonIdeOnlyDeclarationCheckers)
            when {
                platform.isJvm() -> add(JvmDeclarationCheckers)
                platform.isJs() -> add(JsDeclarationCheckers)
                platform.isWasm() -> {
                    add(WasmBaseDeclarationCheckers)
                    // TODO, KT-71596: Add proper selection of either of the following two
                    // add(WasmJsDeclarationCheckers)
                    // add(WasmWasiDeclarationCheckers)
                }
                platform.isNative() -> add(NativeDeclarationCheckers)
                else -> {}
            }
            addAll(extensionCheckers.map { it.declarationCheckers })
        }

        if (filter.runExtraCheckers) {
            add(ExtraDeclarationCheckers)
        }

        if (filter.runExperimentalCheckers) {
            add(ExperimentalDeclarationCheckers)
        }
    }

    fun createExpressionCheckers(
        filter: DiagnosticCheckerFilter,
        platform: TargetPlatform,
        extensionCheckers: List<FirAdditionalCheckersExtension>
    ) = createExpressionCheckers {
        if (filter.runDefaultCheckers) {
            add(CommonExpressionCheckers)
            when {
                platform.isJvm() -> add(JvmExpressionCheckers)
                platform.isJs() -> add(JsExpressionCheckers)
                platform.isWasm() -> {
                    add(WasmBaseExpressionCheckers)
                    // TODO, KT-71596
                    // add(WasmJsExpressionCheckers)
                }
                platform.isNative() -> add(NativeExpressionCheckers)
                else -> {
                }
            }
            addAll(extensionCheckers.map { it.expressionCheckers })
        }

        if (filter.runExtraCheckers) {
            add(ExtraExpressionCheckers)
        }

        if (filter.runExperimentalCheckers) {
            add(ExperimentalExpressionCheckers)
        }
    }

    fun createTypeCheckers(
        filter: DiagnosticCheckerFilter,
        platform: TargetPlatform,
        extensionCheckers: List<FirAdditionalCheckersExtension>,
    ) = createTypeCheckers {
        if (filter.runDefaultCheckers) {
            add(CommonTypeCheckers)
            when {
                platform.isJvm() -> add(JvmTypeCheckers)
                platform.isWasm() -> add(WasmBaseTypeCheckers)
                platform.isNative() -> add(NativeTypeCheckers)
                else -> {}
            }
            addAll(extensionCheckers.map { it.typeCheckers })
        }

        if (filter.runExtraCheckers) {
            add(ExtraTypeCheckers)
        }

        if (filter.runExperimentalCheckers) {
            add(ExperimentalTypeCheckers)
        }
    }


    inline fun createDeclarationCheckers(
        createDeclarationCheckers: MutableList<DeclarationCheckers>.() -> Unit
    ): DeclarationCheckers =
        createDeclarationCheckers(buildList(createDeclarationCheckers))


    @OptIn(CheckersComponentInternal::class)
    fun createDeclarationCheckers(declarationCheckers: List<DeclarationCheckers>): DeclarationCheckers {
        return when (declarationCheckers.size) {
            1 -> declarationCheckers.single()
            else -> ComposedDeclarationCheckers { true }.apply {
                declarationCheckers.forEach(::register)
            }
        }
    }

    inline fun createExpressionCheckers(
        createExpressionCheckers: MutableList<ExpressionCheckers>.() -> Unit
    ): ExpressionCheckers = createExpressionCheckers(buildList(createExpressionCheckers))

    @OptIn(CheckersComponentInternal::class)
    fun createExpressionCheckers(expressionCheckers: List<ExpressionCheckers>): ExpressionCheckers {
        return when (expressionCheckers.size) {
            1 -> expressionCheckers.single()
            else -> ComposedExpressionCheckers { true }.apply {
                expressionCheckers.forEach(::register)
            }
        }
    }

    inline fun createTypeCheckers(
        createTypeCheckers: MutableList<TypeCheckers>.() -> Unit
    ): TypeCheckers = createTypeCheckers(buildList(createTypeCheckers))

    @OptIn(CheckersComponentInternal::class)
    fun createTypeCheckers(typeCheckers: List<TypeCheckers>): TypeCheckers {
        return when (typeCheckers.size) {
            1 -> typeCheckers.single()
            else -> ComposedTypeCheckers { true }.apply {
                typeCheckers.forEach(::register)
            }
        }
    }
}
