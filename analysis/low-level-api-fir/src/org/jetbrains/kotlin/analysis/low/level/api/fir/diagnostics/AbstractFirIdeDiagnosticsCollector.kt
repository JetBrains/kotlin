/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics

import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
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
import org.jetbrains.kotlin.platform.SimplePlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.platform.JsPlatform
import org.jetbrains.kotlin.platform.konan.NativePlatform

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
    ): DiagnosticCollectorComponents {
        val module = session.llFirModuleData.ktModule
        val platform = module.platform.componentPlatforms.first()
        val extensionCheckers = session.extensionService.additionalCheckers
        val declarationCheckers = createDeclarationCheckers(useExtendedCheckers, platform, extensionCheckers)
        val expressionCheckers = createExpressionCheckers(useExtendedCheckers, platform, extensionCheckers)
        val typeCheckers = createTypeCheckers(useExtendedCheckers, platform, extensionCheckers)

        val regularComponents = buildList {
            if (!useExtendedCheckers) {
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
        useExtendedCheckers: Boolean,
        platform: SimplePlatform,
        extensionCheckers: List<FirAdditionalCheckersExtension>
    ): DeclarationCheckers {
        return if (useExtendedCheckers) {
            ExtendedDeclarationCheckers
        } else {
            createDeclarationCheckers {
                add(CommonDeclarationCheckers)
                when (platform) {
                    is JvmPlatform -> add(JvmDeclarationCheckers)
                    is JsPlatform -> add(JsDeclarationCheckers)
                    is NativePlatform -> add(NativeDeclarationCheckers)
                    else -> {}
                }
                addAll(extensionCheckers.map { it.declarationCheckers })
            }
        }
    }

    private fun createExpressionCheckers(
        useExtendedCheckers: Boolean,
        platform: SimplePlatform,
        extensionCheckers: List<FirAdditionalCheckersExtension>
    ): ExpressionCheckers {
        return if (useExtendedCheckers) {
            ExtendedExpressionCheckers
        } else {
            createExpressionCheckers {
                add(CommonExpressionCheckers)
                when (platform) {
                    is JvmPlatform -> add(JvmExpressionCheckers)
                    is JsPlatform -> add(JsExpressionCheckers)
                    else -> {
                    }
                }
                addAll(extensionCheckers.map { it.expressionCheckers })
            }
        }
    }

    private fun createTypeCheckers(useExtendedCheckers: Boolean, platform: SimplePlatform, extensionCheckers: List<FirAdditionalCheckersExtension>): TypeCheckers {
        if (useExtendedCheckers) return ExtendedTypeCheckers
        return createTypeCheckers {
            add(CommonTypeCheckers)
            when (platform) {
                is JvmPlatform -> add(JvmTypeCheckers)
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
            else -> ComposedDeclarationCheckers().apply {
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
            else -> ComposedExpressionCheckers().apply {
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
            else -> ComposedTypeCheckers().apply {
                typeCheckers.forEach(::register)
            }
        }
    }
}
