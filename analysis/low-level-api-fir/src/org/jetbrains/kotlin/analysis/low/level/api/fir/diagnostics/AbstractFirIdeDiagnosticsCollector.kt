/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics

import com.intellij.util.SmartFMap
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.LLCheckersFactory.Provider.Companion.filterToCheckersMapUpdater
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.platform.LLPlatformCheckersConfiguration
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.diagnostics.PendingDiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.analysis.CheckersComponentInternal
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.ComposedDeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckersDiagnosticComponent
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FilteredDeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ComposedExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckersDiagnosticComponent
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FilteredExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.type.ComposedTypeCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.type.FilteredTypeCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.type.TypeCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.type.TypeCheckersDiagnosticComponent
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.analysis.collectors.DiagnosticCollectorComponents
import org.jetbrains.kotlin.fir.analysis.collectors.components.ControlFlowAnalysisDiagnosticComponent
import org.jetbrains.kotlin.fir.analysis.collectors.components.ErrorNodeDiagnosticCollectorComponent
import org.jetbrains.kotlin.fir.analysis.collectors.components.ReportCommitterDiagnosticComponent
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.analysis.extensions.additionalCheckers
import org.jetbrains.kotlin.fir.extensions.extensionService
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
    /**
     * In a metadata session, we currently only run [metadata-ready][FirCheckerWithMppKind.platformSpecificCheckerEnabledInMetadataCompilation]
     * checkers. For the full support of all diagnostics reported by the compiler on that common code, we would also have to run
     * platform-specific checkers from each leaf platform module. This is because the compiler reports additional diagnostics on common code
     * when compiling the code for a specific platform. See KT-82245.
     *
     * By running metadata-ready checkers, we already support a subset of the platform-specific diagnostics. As more checkers become
     * metadata-ready, the gap should close to some degree.
     */
    private val platformCheckersConfigurations by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LLPlatformCheckersConfiguration.forPlatform(session.llFirModuleData.platform)
    }

    private val declarationCheckersProvider = Provider(session, ::createDeclarationCheckers)
    private val expressionCheckersProvider = Provider(session, ::createExpressionCheckers)
    private val typeCheckersProvider = Provider(session, ::createTypeCheckers)

    fun createComponents(filter: DiagnosticCheckerFilter, reporter: PendingDiagnosticReporter): DiagnosticCollectorComponents {
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
            val additionalCheckers = session.extensionService.additionalCheckers
            return checkersFactory(filter, additionalCheckers)
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
        extensionCheckers: List<FirAdditionalCheckersExtension>,
    ) = createDeclarationCheckers {
        if (filter.runDefaultCheckers) {
            add(CommonDeclarationCheckers)
            add(CommonIdeOnlyDeclarationCheckers)

            platformCheckersConfigurations
                .flatMap { it.declarationCheckers }
                .forEach { add(it.onlyEnabledCheckers()) }

            addAll(extensionCheckers.map { it.declarationCheckers })
        }

        if (filter.runExtraCheckers) {
            add(ExtraDeclarationCheckers)

            platformCheckersConfigurations
                .flatMap { it.extraDeclarationCheckers }
                .forEach { add(it.onlyEnabledCheckers()) }
        }
    }

    private fun DeclarationCheckers.onlyEnabledCheckers(): DeclarationCheckers =
        if (session.isMetadataSession) FilteredDeclarationCheckers(this) { it.platformSpecificCheckerEnabledInMetadataCompilation }
        else this

    private fun createExpressionCheckers(
        filter: DiagnosticCheckerFilter,
        extensionCheckers: List<FirAdditionalCheckersExtension>,
    ) = createExpressionCheckers {
        if (filter.runDefaultCheckers) {
            add(CommonExpressionCheckers)

            platformCheckersConfigurations
                .flatMap { it.expressionCheckers }
                .forEach { add(it.onlyEnabledCheckers()) }

            addAll(extensionCheckers.map { it.expressionCheckers })
        }

        if (filter.runExtraCheckers) {
            add(ExtraExpressionCheckers)

            platformCheckersConfigurations
                .flatMap { it.extraExpressionCheckers }
                .forEach { add(it.onlyEnabledCheckers()) }
        }

        if (filter.runExperimentalCheckers) {
            add(ExperimentalExpressionCheckers)
        }
    }

    private fun ExpressionCheckers.onlyEnabledCheckers(): ExpressionCheckers =
        if (session.isMetadataSession) FilteredExpressionCheckers(this) { it.platformSpecificCheckerEnabledInMetadataCompilation }
        else this

    private fun createTypeCheckers(
        filter: DiagnosticCheckerFilter,
        extensionCheckers: List<FirAdditionalCheckersExtension>,
    ) = createTypeCheckers {
        if (filter.runDefaultCheckers) {
            add(CommonTypeCheckers)

            platformCheckersConfigurations
                .flatMap { it.typeCheckers }
                .forEach { add(it.onlyEnabledCheckers()) }

            addAll(extensionCheckers.map { it.typeCheckers })
        }

        if (filter.runExtraCheckers) {
            platformCheckersConfigurations
                .flatMap { it.extraTypeCheckers }
                .forEach { add(it.onlyEnabledCheckers()) }
        }

        if (filter.runExperimentalCheckers) {
            add(ExperimentalTypeCheckers)
        }
    }

    private fun TypeCheckers.onlyEnabledCheckers(): TypeCheckers =
        if (session.isMetadataSession) FilteredTypeCheckers(this) { it.platformSpecificCheckerEnabledInMetadataCompilation }
        else this

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
