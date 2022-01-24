/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics

import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.module
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.CheckersComponentInternal
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.ComposedDeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ComposedExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.type.TypeCheckers
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.analysis.collectors.components.*
import org.jetbrains.kotlin.fir.analysis.js.checkers.JsDeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.js.checkers.JsExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.JvmDeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.JvmExpressionCheckers
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.platform.SimplePlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatform

internal abstract class AbstractLLFirDiagnosticsCollector(
    session: FirSession,
    useExtendedCheckers: Boolean,
) : AbstractDiagnosticCollector(
    session,
    createComponents = { reporter ->
        CheckersFactory.createComponents(session, reporter, useExtendedCheckers)
    }
)


private object CheckersFactory {
    fun createComponents(
        session: FirSession,
        reporter: DiagnosticReporter,
        useExtendedCheckers: Boolean
    ): List<AbstractDiagnosticCollectorComponent> {
        val module = session.moduleData.module
        val platform = module.platform.componentPlatforms.first()
        val declarationCheckers = createDeclarationCheckers(useExtendedCheckers, platform)
        val expressionCheckers = createExpressionCheckers(useExtendedCheckers, platform)
        val typeCheckers = createTypeCheckers(useExtendedCheckers)

        @OptIn(ExperimentalStdlibApi::class)
        return buildList {
            if (!useExtendedCheckers) {
                add(ErrorNodeDiagnosticCollectorComponent(session, reporter))
            }
            add(DeclarationCheckersDiagnosticComponent(session, reporter, declarationCheckers))
            add(ExpressionCheckersDiagnosticComponent(session, reporter, expressionCheckers))
            typeCheckers?.let { add(TypeCheckersDiagnosticComponent(session, reporter, it)) }
            add(ControlFlowAnalysisDiagnosticComponent(session, reporter, declarationCheckers))
        }
    }


    private fun createDeclarationCheckers(useExtendedCheckers: Boolean, platform: SimplePlatform): DeclarationCheckers {
        return if (useExtendedCheckers) {
            ExtendedDeclarationCheckers
        } else {
            createDeclarationCheckers {
                add(CommonDeclarationCheckers)
                when (platform) {
                    is JvmPlatform -> add(JvmDeclarationCheckers)
                    // TODO: JsPlatform is inaccessible due to module's dependencies
                    else -> add(JsDeclarationCheckers)
                }
            }
        }
    }

    private fun createExpressionCheckers(useExtendedCheckers: Boolean, platform: SimplePlatform): ExpressionCheckers {
        return if (useExtendedCheckers) {
            ExtendedExpressionCheckers
        } else {
            createExpressionCheckers {
                add(CommonExpressionCheckers)
                when (platform) {
                    is JvmPlatform -> add(JvmExpressionCheckers)
                    // TODO: JsPlatform is inaccessible due to module's dependencies
                    else -> add(JsExpressionCheckers)
                }
            }
        }
    }

    private fun createTypeCheckers(useExtendedCheckers: Boolean): TypeCheckers? =
        if (useExtendedCheckers) ExtendedTypeCheckers else CommonTypeCheckers


    @OptIn(ExperimentalStdlibApi::class)
    private inline fun createDeclarationCheckers(
        createDeclarationCheckers: MutableList<DeclarationCheckers>.() -> Unit
    ): DeclarationCheckers =
        createDeclarationCheckers(buildList(createDeclarationCheckers))


    @OptIn(CheckersComponentInternal::class)
    private fun createDeclarationCheckers(declarationCheckers: List<DeclarationCheckers>): DeclarationCheckers {
        return when (declarationCheckers.size) {
            1 -> declarationCheckers.single()
            else -> ComposedDeclarationCheckers().apply {
                declarationCheckers.forEach(::register)
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private inline fun createExpressionCheckers(
        createExpressionCheckers: MutableList<ExpressionCheckers>.() -> Unit
    ): ExpressionCheckers = createExpressionCheckers(buildList(createExpressionCheckers))

    @OptIn(CheckersComponentInternal::class)
    private fun createExpressionCheckers(expressionCheckers: List<ExpressionCheckers>): ExpressionCheckers {
        return when (expressionCheckers.size) {
            1 -> expressionCheckers.single()
            else -> ComposedExpressionCheckers().apply {
                expressionCheckers.forEach(::register)
            }
        }
    }
}
