/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.resolvedStatus
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.resolve.dependencies.ClinitIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.DependencyGraphAnalyzer
import org.jetbrains.kotlin.fir.resolve.dependencies.EnclosingEntity
import org.jetbrains.kotlin.fir.resolve.dependencies.EnumEntryIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.QualifierIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.StaticAnonymousInitializerIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.StaticPropertyIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.dependencyGraphAnalyzer
import org.jetbrains.kotlin.fir.resolve.dependencies.logic.dependencyGraphResolver
import org.jetbrains.kotlin.fir.symbols.SymbolInternals

object FirStaticInitializationChecker : FirFileChecker(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFile) {
        val resolver = context.session.dependencyGraphResolver ?: return
        val analyzer = context.session.dependencyGraphAnalyzer ?: return
        val pendingNodes = resolver.collectDependencies(declaration)
        for (node in pendingNodes) {
            when (node) {
                is ClinitIndex -> analyzer.checkDeadlocks(node.enclosingEntity)
                is QualifierIndex -> {
                    analyzer.checkObjectConstructor(node.enclosingEntity)
                    analyzer.checkDeadlocks(node.enclosingEntity)
                }
                is EnumEntryIndex -> analyzer.checkEnumEntry(node.enclosingEntity)
                is StaticPropertyIndex -> analyzer.checkProperty(node)
                is StaticAnonymousInitializerIndex -> analyzer.checkAccessesInInitializer(node)
                else -> {}
            }
        }
    }

    @OptIn(SymbolInternals::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    fun DependencyGraphAnalyzer.checkDeadlocks(enclosingEntity: EnclosingEntity<FirRegularClass>) {
        if (enclosingEntity.symbol.resolvedStatus?.isCompanion ?: false) return
        val deadlockingEntities = mutuallyDependentEntities(enclosingEntity).toList()
        if (deadlockingEntities.isNotEmpty()) {
            reporter.reportOn(
                enclosingEntity.symbol.fir.source,
                FirErrors.POSSIBLE_INITIALIZATION_DEADLOCK,
                deadlockingEntities.map(EnclosingEntity<*>::symbol)
            )
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    fun DependencyGraphAnalyzer.checkObjectConstructor(enclosingEntity: EnclosingEntity.Object) {
        val index = enclosingEntity.beginInitializationIndex
        if (isPoisoned(index)) {
            collectAllPoisoningDirectAccesses(index).forEach { access ->
                reporter.reportOn(access, FirErrors.POTENTIALLY_UNINITIALIZED_ACCESS)
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    fun DependencyGraphAnalyzer.checkAccessesInInitializer(initializerNode: StaticAnonymousInitializerIndex) {
        if (isPoisoned(initializerNode)) {
            collectAllPoisoningDirectAccesses(initializerNode).forEach { access ->
                reporter.reportOn(access, FirErrors.POTENTIALLY_UNINITIALIZED_ACCESS)
            }
        }
    }

    @OptIn(SymbolInternals::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    fun DependencyGraphAnalyzer.checkEnumEntry(enclosingEntity: EnclosingEntity.EnumEntry) {
        val enumEntryIndex = enclosingEntity.beginInitializationIndex
        if (isPoisoned(enumEntryIndex)) {
            reporter.reportOn(
                enclosingEntity.symbol.fir.source,
                FirErrors.POTENTIALLY_UNINITIALIZED_PROPERTY,
                mutuallyDependentEntities(enclosingEntity).mapTo(mutableListOf(), EnclosingEntity<*>::symbol)
            )
            collectAllPoisoningDirectAccesses(enumEntryIndex).forEach { access ->
                reporter.reportOn(access, FirErrors.POTENTIALLY_UNINITIALIZED_ACCESS)
            }
        }
    }

    @OptIn(SymbolInternals::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    fun DependencyGraphAnalyzer.checkProperty(propertyNode: StaticPropertyIndex) {
        if (isPoisoned(propertyNode)) {
            reporter.reportOn(
                propertyNode.symbol.fir.source,
                FirErrors.POTENTIALLY_UNINITIALIZED_PROPERTY,
                mutuallyDependentEntities(propertyNode.enclosingEntity).mapTo(mutableListOf(), EnclosingEntity<*>::symbol)
            )
            collectAllPoisoningDirectAccesses(propertyNode).forEach { access ->
                reporter.reportOn(access, FirErrors.POTENTIALLY_UNINITIALIZED_ACCESS)
            }
        }
    }
}
