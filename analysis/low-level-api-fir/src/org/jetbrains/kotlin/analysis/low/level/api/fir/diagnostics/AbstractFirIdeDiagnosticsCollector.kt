/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics

import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.llFirModuleData
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.CheckersComponentInternal
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.ComposedDeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ComposedExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.type.ComposedTypeCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.type.TypeCheckers
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
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative

internal abstract class AbstractLLFirDiagnosticsCollector(
    session: FirSession,
    useExtraCheckers: Boolean,
) : AbstractDiagnosticCollector(
    session,
    createComponents = { reporter ->
        CheckersFactory.createComponents(session, reporter, useExtraCheckers)
    }
)


private object CheckersFactory {
    fun createComponents(
        session: FirSession,
        reporter: DiagnosticReporter,
        useExtraCheckers: Boolean
    ): DiagnosticCollectorComponents {
        val module = session.llFirModuleData.ktModule
        val platform = module.targetPlatform
        val extensionCheckers = session.extensionService.additionalCheckers
        val declarationCheckers = createDeclarationCheckers(useExtraCheckers, platform, extensionCheckers)
        val expressionCheckers = createExpressionCheckers(useExtraCheckers, platform, extensionCheckers)
        val typeCheckers = createTypeCheckers(useExtraCheckers, platform, extensionCheckers)

        val regularComponents = buildList {
            if (!useExtraCheckers) {
                add(ErrorNodeDiagnosticCollectorComponent(session, reporter))
            }
            add(DeclarationCheckersDiagnosticComponent(session, reporter, declarationCheckers))
            add(ExpressionCheckersDiagnosticComponent(session, reporter, expressionCheckers))
            add(TypeCheckersDiagnosticComponent(session, reporter, typeCheckers))
            add(ControlFlowAnalysisDiagnosticComponent(session, reporter, declarationCheckers))
        }
        return DiagnosticCollectorComponents(regularComponents, ReportCommitterDiagnosticComponent(session, reporter))
    }


    private fun createDeclarationCheckers(
        useExtraCheckers: Boolean,
        platform: TargetPlatform,
        extensionCheckers: List<FirAdditionalCheckersExtension>
    ): DeclarationCheckers {
        return if (useExtraCheckers) {
            ExtraDeclarationCheckers
        } else {
            createDeclarationCheckers {
                add(CommonDeclarationCheckers)
                add(CommonIdeOnlyDeclarationCheckers)
                when {
                    platform.isJvm() -> add(JvmDeclarationCheckers)
                    platform.isJs() -> add(JsDeclarationCheckers)
                    platform.isNative() -> add(NativeDeclarationCheckers)
                    else -> {}
                }
                addAll(extensionCheckers.map { it.declarationCheckers })
            }
        }
    }

    private fun createExpressionCheckers(
        useExtraCheckers: Boolean,
        platform: TargetPlatform,
        extensionCheckers: List<FirAdditionalCheckersExtension>
    ): ExpressionCheckers {
        return if (useExtraCheckers) {
            ExtraExpressionCheckers
        } else {
            createExpressionCheckers {
                add(CommonExpressionCheckers)
                when {
                    platform.isJvm() -> add(JvmExpressionCheckers)
                    platform.isJs() -> add(JsExpressionCheckers)
                    else -> {
                    }
                }
                addAll(extensionCheckers.map { it.expressionCheckers })
            }
        }
    }

    private fun createTypeCheckers(
        useExtraCheckers: Boolean,
        platform: TargetPlatform,
        extensionCheckers: List<FirAdditionalCheckersExtension>,
    ): TypeCheckers {
        if (useExtraCheckers) return ExtraTypeCheckers
        return createTypeCheckers {
            add(CommonTypeCheckers)
            when {
                platform.isJvm() -> add(JvmTypeCheckers)
                else -> {}
            }
            addAll(extensionCheckers.map { it.typeCheckers })
        }
    }


    private inline fun createDeclarationCheckers(
        createDeclarationCheckers: MutableList<DeclarationCheckers>.() -> Unit
    ): DeclarationCheckers =
        createDeclarationCheckers(buildList(createDeclarationCheckers))


    @OptIn(CheckersComponentInternal::class)
    private fun createDeclarationCheckers(declarationCheckers: List<DeclarationCheckers>): DeclarationCheckers {
        return when (declarationCheckers.size) {
            1 -> declarationCheckers.single()
            else -> ComposedDeclarationCheckers { true }.apply {
                declarationCheckers.forEach(::register)
            }
        }
    }

    private inline fun createExpressionCheckers(
        createExpressionCheckers: MutableList<ExpressionCheckers>.() -> Unit
    ): ExpressionCheckers = createExpressionCheckers(buildList(createExpressionCheckers))

    @OptIn(CheckersComponentInternal::class)
    private fun createExpressionCheckers(expressionCheckers: List<ExpressionCheckers>): ExpressionCheckers {
        return when (expressionCheckers.size) {
            1 -> expressionCheckers.single()
            else -> ComposedExpressionCheckers { true }.apply {
                expressionCheckers.forEach(::register)
            }
        }
    }

    private inline fun createTypeCheckers(
        createTypeCheckers: MutableList<TypeCheckers>.() -> Unit
    ): TypeCheckers = createTypeCheckers(buildList(createTypeCheckers))

    @OptIn(CheckersComponentInternal::class)
    private fun createTypeCheckers(typeCheckers: List<TypeCheckers>): TypeCheckers {
        return when (typeCheckers.size) {
            1 -> typeCheckers.single()
            else -> ComposedTypeCheckers { true }.apply {
                typeCheckers.forEach(::register)
            }
        }
    }
}
