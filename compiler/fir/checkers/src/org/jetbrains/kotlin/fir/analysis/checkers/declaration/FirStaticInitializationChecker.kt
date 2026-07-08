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
import org.jetbrains.kotlin.fir.resolve.dependencies.AnalysisResult
import org.jetbrains.kotlin.fir.resolve.dependencies.AnonymousInitializerIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.ClinitIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.DependencyGraphAnalyzer
import org.jetbrains.kotlin.fir.resolve.dependencies.EnclosingEntity
import org.jetbrains.kotlin.fir.resolve.dependencies.EnclosingEntity.Companion.parentEnclosingEntityOrSelf
import org.jetbrains.kotlin.fir.resolve.dependencies.EnumEntryIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.FunctionIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.InitializationCycleAccessResult
import org.jetbrains.kotlin.fir.resolve.dependencies.PropertyIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.QualifierIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.dependencyGraphAnalyzer
import org.jetbrains.kotlin.fir.resolve.dependencies.logic.dependencyGraphResolver
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.name.CallableId

object FirStaticInitializationChecker : FirFileChecker(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFile) {
        val resolver = context.session.dependencyGraphResolver ?: return
        val analyzer = context.session.dependencyGraphAnalyzer ?: return
        resolver.collectDependencies(declaration).forEach { node ->
            when (node) {
                is ClinitIndex -> analyzer.checkDeadlocks(node.enclosingEntity)
                is QualifierIndex -> {
                    analyzer.checkObjectConstructor(node.enclosingEntity)
                    analyzer.checkDeadlocks(node.enclosingEntity)
                }
                is EnumEntryIndex -> analyzer.checkEnumEntry(node.enclosingEntity)
                is PropertyIndex -> analyzer.checkProperty(node)
                is AnonymousInitializerIndex -> analyzer.checkAccessesInInitializer(node)
                else -> {}
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportResultAndPossibleUninitialization(result: AnalysisResult): Boolean {
        val [type, accesses] = result
        when (type) {
            is InitializationCycleAccessResult.UninitializedPropertyAccess -> accesses.forEach {
                reporter.reportOn(it, FirErrors.ACCESSING_POSSIBLY_UNINITIALIZED_PROPERTY, type.node.name)
            }
            is InitializationCycleAccessResult.UninitializedEnumEntryAccess -> accesses.forEach {
                reporter.reportOn(it, FirErrors.ACCESSING_POSSIBLY_UNINITIALIZED_ENUM_ENTRY, type.node.enclosingEntity.symbol)
            }
            is InitializationCycleAccessResult.CyclicAccess -> accesses.forEach {
                reporter.reportOn(it, FirErrors.POSSIBLE_CYCLIC_ACCESS, type.node.symbol)
            }
            is InitializationCycleAccessResult.InaccessibleEntityAccess ->
                when (val node = type.node) {
                    is QualifierIndex -> accesses.forEach {
                        reporter.reportOn(it, FirErrors.ACCESSING_POSSIBLY_INACCESSIBLE_OBJECT_REFERENCE, type.entity.name)
                    }
                    is EnumEntryIndex -> {
                        val enumClass = node.enclosingEntity.parentEnclosingEntity
                        accesses.forEach {
                            reporter.reportOn(it, FirErrors.ACCESSING_DECLARATION_OF_POSSIBLY_INACCESSIBLE_CLASS, enumClass.name, node.enclosingEntity.symbol)
                        }
                    }
                    is FunctionIndex<*>, is PropertyIndex -> {
                        val parent = type.entity.parentEnclosingEntityOrSelf
                        accesses.forEach {
                            reporter.reportOn(it, FirErrors.ACCESSING_DECLARATION_OF_POSSIBLY_INACCESSIBLE_CLASS, parent.name, node.symbol)
                        }
                    }
                }
            is InitializationCycleAccessResult.DeadlockInducingConstructorCall -> accesses.forEach {
                reporter.reportOn(it, FirErrors.CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS, type.node.symbol.callableId.classId?.relativeClassName ?: CallableId(type.node.symbol.name).asSingleFqName())
            }
            else -> {}
        }
        return type.poisonsInitializers
    }

    @OptIn(SymbolInternals::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun DependencyGraphAnalyzer.checkDeadlocks(enclosingEntity: EnclosingEntity<FirRegularClass>) {
        if (enclosingEntity.symbol.resolvedStatus?.isCompanion ?: false) return
        val deadlockingEntities = mutuallyDependentEntities(enclosingEntity).toList()
        if (deadlockingEntities.isNotEmpty()) {
            reporter.reportOn(
                enclosingEntity.symbol.fir.source,
                FirErrors.POSSIBLE_INITIALIZATION_DEADLOCK,
                deadlockingEntities.map(EnclosingEntity<*>::name)
            )
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun DependencyGraphAnalyzer.checkObjectConstructor(enclosingEntity: EnclosingEntity.Object) =
        analyze(enclosingEntity.beginInitializationIndex).forEach { reportResultAndPossibleUninitialization(it) }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun DependencyGraphAnalyzer.checkAccessesInInitializer(initializerNode: AnonymousInitializerIndex) =
        analyze(initializerNode).forEach { reportResultAndPossibleUninitialization(it) }

    @OptIn(SymbolInternals::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun DependencyGraphAnalyzer.checkEnumEntry(enclosingEntity: EnclosingEntity.EnumEntry) {
        val isPossiblyUninitialized = analyze(enclosingEntity.beginInitializationIndex).fold(false) { isUninitialized, result ->
            isUninitialized || reportResultAndPossibleUninitialization(result)
        }
        if (isPossiblyUninitialized) {
            reporter.reportOn(
                enclosingEntity.symbol.fir.source,
                FirErrors.POSSIBLY_UNINITIALIZED_ENUM_ENTRY,
                enclosingEntity.symbol,
                mutuallyDependentEntities(enclosingEntity).mapTo(mutableListOf(), EnclosingEntity<*>::name)
            )
        }
    }

    @OptIn(SymbolInternals::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    fun DependencyGraphAnalyzer.checkProperty(propertyNode: PropertyIndex) {
        val isPossiblyUninitialized = analyze(propertyNode).fold(false) { isUninitialized, result ->
            isUninitialized || reportResultAndPossibleUninitialization(result)
        }
        if (isPossiblyUninitialized) {
            reporter.reportOn(
                propertyNode.symbol.fir.source,
                FirErrors.POSSIBLY_UNINITIALIZED_PROPERTY,
                propertyNode.name,
                propertyNode.enclosingEntity?.let {
                    mutuallyDependentEntities(it).mapTo(mutableListOf(), EnclosingEntity<*>::name)
                } ?: emptyList()
            )
        }
    }
}
