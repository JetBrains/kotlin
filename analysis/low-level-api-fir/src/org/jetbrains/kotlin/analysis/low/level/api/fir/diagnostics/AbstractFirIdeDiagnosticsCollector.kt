/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics

import com.intellij.util.SmartFMap
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.LLCheckersFactory.Provider.Companion.filterToCheckersMapUpdater
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
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
import org.jetbrains.kotlin.fir.analysis.collectors.components.ControlFlowAnalysisDiagnosticComponent
import org.jetbrains.kotlin.fir.analysis.collectors.components.ErrorNodeDiagnosticCollectorComponent
import org.jetbrains.kotlin.fir.analysis.collectors.components.ReportCommitterDiagnosticComponent
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
import org.jetbrains.kotlin.platform.wasm.isWasmJs
import org.jetbrains.kotlin.platform.wasm.isWasmWasi
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

internal abstract class AbstractLLFirDiagnosticsCollector(
    session: FirSession,
    filter: DiagnosticCheckerFilter,
) : AbstractDiagnosticCollector(
    session,
    createComponents = { reporter ->
        session.checkersFactory.createComponents(filter, reporter)
    }
)

private val FirSession.checkersFactory: LLCheckersFactory by FirSession.sessionComponentAccessor()

/**
 * In the CLI mode all checkers are created once during the session initialization phase.
 * In the Analysis API checkers depend on [DiagnosticCheckerFilter].
 *
 * This factory provides an efficient way to get checkers for a given filter.
 *
 * @see org.jetbrains.kotlin.fir.analysis.CheckersComponent
 */
internal class LLCheckersFactory(val session: LLFirSession) : FirSessionComponent {
    private val declarationCheckersProvider = Provider(session, ::createDeclarationCheckers)
    private val expressionCheckersProvider = Provider(session, ::createExpressionCheckers)
    private val typeCheckersProvider = Provider(session, ::createTypeCheckers)

    fun createComponents(filter: DiagnosticCheckerFilter, reporter: DiagnosticReporter): DiagnosticCollectorComponents {
        val declarationCheckers = declarationCheckersProvider.getOrCreateCheckers(filter)
        val expressionCheckers = expressionCheckersProvider.getOrCreateCheckers(filter)
        val typeCheckers = typeCheckersProvider.getOrCreateCheckers(filter)

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

    /**
     * This provider allows creating checkers lazily based on a given [filter][DiagnosticCheckerFilter].
     */
    private class Provider<T>(
        private val session: FirSession,
        private val checkersFactory: (
            filter: DiagnosticCheckerFilter,
            platform: TargetPlatform,
            additionalCheckers: List<FirAdditionalCheckersExtension>,
        ) -> T,
    ) {
        /** @see filterToCheckersMapUpdater */
        @Volatile
        private var filterToCheckersMap: SmartFMap<DiagnosticCheckerFilter, T> = SmartFMap.emptyMap()

        fun getOrCreateCheckers(filter: DiagnosticCheckerFilter): T {
            // Happy-path to avoid checkers recreation
            filterToCheckersMap[filter]?.let { return it }

            val checkers = createCheckers(filter)
            do {
                val oldMap = filterToCheckersMap
                oldMap[filter]?.let { return it }

                val newMap = oldMap.plus(filter, checkers)
            } while (!filterToCheckersMapUpdater.compareAndSet(/* obj = */ this, /* expect = */ oldMap, /* update = */ newMap))

            return checkers
        }

        private fun createCheckers(filter: DiagnosticCheckerFilter): T {
            val platform = session.llFirModuleData.platform
            val additionalCheckers = session.extensionService.additionalCheckers
            return checkersFactory(filter, platform, additionalCheckers)
        }

        companion object {
            private val filterToCheckersMapUpdater = AtomicReferenceFieldUpdater.newUpdater(
                /* tclass = */ Provider::class.java,
                /* vclass = */ SmartFMap::class.java,
                /* fieldName = */ "filterToCheckersMap",
            )
        }
    }

    private fun createDeclarationCheckers(
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
                    if (platform.isWasmJs()) add(WasmJsDeclarationCheckers)
                    if (platform.isWasmWasi()) add(WasmWasiDeclarationCheckers)
                }
                platform.isNative() -> add(NativeDeclarationCheckers)
                else -> {}
            }
            addAll(extensionCheckers.map { it.declarationCheckers })
        }

        if (filter.runExtraCheckers) {
            add(ExtraDeclarationCheckers)
        }
    }

    private fun createExpressionCheckers(
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
                    if (platform.isWasmJs()) add(WasmJsExpressionCheckers)
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

    private fun createTypeCheckers(
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

        if (filter.runExperimentalCheckers) {
            add(ExperimentalTypeCheckers)
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
