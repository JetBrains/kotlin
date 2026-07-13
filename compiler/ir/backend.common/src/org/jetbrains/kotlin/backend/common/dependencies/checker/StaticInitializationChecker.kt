/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.dependencies.checker

import org.jetbrains.kotlin.KtOffsetsOnlySourceElement
import org.jetbrains.kotlin.backend.common.dependencies.AnonymousInitializerIndex
import org.jetbrains.kotlin.backend.common.dependencies.ClinitIndex
import org.jetbrains.kotlin.backend.common.dependencies.DependencyGraph
import org.jetbrains.kotlin.backend.common.dependencies.EnumEntryIndex
import org.jetbrains.kotlin.backend.common.dependencies.FunctionIndex
import org.jetbrains.kotlin.backend.common.dependencies.InitializationCycleAccessResult
import org.jetbrains.kotlin.backend.common.dependencies.PropertyIndex
import org.jetbrains.kotlin.backend.common.dependencies.QualifierIndex
import org.jetbrains.kotlin.backend.common.dependencies.checker.StaticInitializationDiagnostics.ACCESSING_DECLARATION_OF_POSSIBLY_INACCESSIBLE_CLASS
import org.jetbrains.kotlin.backend.common.dependencies.checker.StaticInitializationDiagnostics.ACCESSING_POSSIBLY_INACCESSIBLE_OBJECT_REFERENCE
import org.jetbrains.kotlin.backend.common.dependencies.checker.StaticInitializationDiagnostics.ACCESSING_POSSIBLY_UNINITIALIZED_ENUM_ENTRY
import org.jetbrains.kotlin.backend.common.dependencies.checker.StaticInitializationDiagnostics.ACCESSING_POSSIBLY_UNINITIALIZED_PROPERTY
import org.jetbrains.kotlin.backend.common.dependencies.checker.StaticInitializationDiagnostics.CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS
import org.jetbrains.kotlin.backend.common.dependencies.checker.StaticInitializationDiagnostics.POSSIBLE_CYCLIC_ACCESS
import org.jetbrains.kotlin.backend.common.dependencies.checker.StaticInitializationDiagnostics.POSSIBLE_INITIALIZATION_DEADLOCK
import org.jetbrains.kotlin.backend.common.dependencies.checker.StaticInitializationDiagnostics.POSSIBLY_UNINITIALIZED_ENUM_ENTRY
import org.jetbrains.kotlin.backend.common.dependencies.checker.StaticInitializationDiagnostics.POSSIBLY_UNINITIALIZED_PROPERTY
import org.jetbrains.kotlin.backend.common.dependencies.logic.DependencyGraphResolver
import org.jetbrains.kotlin.backend.common.dependencies.model.AnalysisResult
import org.jetbrains.kotlin.backend.common.dependencies.model.DependencyGraphAnalyzer
import org.jetbrains.kotlin.backend.common.dependencies.model.EnclosingEntity
import org.jetbrains.kotlin.backend.common.dependencies.model.EnclosingEntity.Companion.parentEnclosingEntityOrSelf
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.util.callableId
import org.jetbrains.kotlin.name.CallableId
import kotlin.sequences.forEach

object StaticInitializationChecker : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val graph = DependencyGraph(moduleFragment)
        val resolver = DependencyGraphResolver(graph)
        val analyzer = DependencyGraphAnalyzer(graph)
        moduleFragment.files.forEach { file ->
            context(pluginContext.diagnosticReporter, file) {
                resolver.collectDependencies(file).forEach { node ->
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
        }
    }

    private fun IrElement.sourceElement(): KtOffsetsOnlySourceElement? = when (this) {
        is IrFunctionAccessExpression -> {
            fun IrElement.actualStartOffset(): Int = when (this) {
                is IrFunctionAccessExpression -> (symbol.owner.parameters.find { it.kind == IrParameterKind.ExtensionReceiver }
                    ?.let(arguments::get) ?: dispatchReceiver)?.actualStartOffset() ?: startOffset
                is IrMemberAccessExpression<*> -> dispatchReceiver?.actualStartOffset() ?: startOffset
                else -> startOffset
            }

            val startOffset = actualStartOffset()
            if (startOffset >= 0) KtOffsetsOnlySourceElement(startOffset, endOffset) else null
        }
        else -> if (startOffset >= 0) KtOffsetsOnlySourceElement(startOffset, endOffset) else null
    }

    context(reporter: IrDiagnosticReporter, containingFile: IrFile)
    private fun reportResultAndPossibleUninitialization(result: AnalysisResult): Boolean {
        val [type, accesses] = result
        when (type) {
            is InitializationCycleAccessResult.UninitializedPropertyAccess -> accesses.forEach {
                reporter.at(it.sourceElement(), it, containingFile).report(ACCESSING_POSSIBLY_UNINITIALIZED_PROPERTY, type.node.name)
            }
            is InitializationCycleAccessResult.UninitializedEnumEntryAccess -> accesses.forEach {
                reporter.at(it.sourceElement(), it, containingFile).report(ACCESSING_POSSIBLY_UNINITIALIZED_ENUM_ENTRY, type.node.enclosingEntity.symbol)
            }
            is InitializationCycleAccessResult.CyclicAccess -> accesses.forEach {
                reporter.at(it.sourceElement(), it, containingFile).report(POSSIBLE_CYCLIC_ACCESS, type.node.symbol)
            }
            is InitializationCycleAccessResult.InaccessibleEntityAccess ->
                when (val node = type.node) {
                    is QualifierIndex -> accesses.forEach {
                        reporter.at(it.sourceElement(), it, containingFile).report(ACCESSING_POSSIBLY_INACCESSIBLE_OBJECT_REFERENCE, type.entity.name)
                    }
                    is EnumEntryIndex -> {
                        val enumClass = node.enclosingEntity.parentEnclosingEntity
                        accesses.forEach {
                            reporter.at(it.sourceElement(), it, containingFile)
                                .report(ACCESSING_DECLARATION_OF_POSSIBLY_INACCESSIBLE_CLASS, enumClass.name, node.enclosingEntity.symbol)
                        }
                    }
                    is FunctionIndex<*> -> {
                        val parent = type.entity.parentEnclosingEntityOrSelf
                        accesses.forEach {
                            reporter.at(it.sourceElement(), it, containingFile)
                                .report(ACCESSING_DECLARATION_OF_POSSIBLY_INACCESSIBLE_CLASS, parent.name, node.symbol)
                        }
                    }
                    is PropertyIndex -> {
                        val parent = type.entity.parentEnclosingEntityOrSelf
                        accesses.forEach {
                            reporter.at(it.sourceElement(), it, containingFile)
                                .report(ACCESSING_DECLARATION_OF_POSSIBLY_INACCESSIBLE_CLASS, parent.name, node.symbol)
                        }
                    }
                }
            is InitializationCycleAccessResult.DeadlockInducingConstructorCall -> accesses.forEach {
                reporter.at(it.sourceElement(), it, containingFile).report(
                    CONSTRUCTING_POSSIBLY_DEADLOCKING_CLASS,
                    type.node.symbol.owner.callableId.classId?.relativeClassName ?: CallableId(type.node.symbol.owner.name).asSingleFqName()
                )
            }
            else -> {}
        }
        return type.poisonsInitializers
    }

    context(reporter: IrDiagnosticReporter, containingFile: IrFile)
    private fun DependencyGraphAnalyzer.checkDeadlocks(enclosingEntity: EnclosingEntity<IrClass>) {
        if (enclosingEntity.symbol.owner.isCompanion) return
        val deadlockingEntities = mutuallyDependentEntities(enclosingEntity).toList()
        if (deadlockingEntities.isNotEmpty()) {
            reporter.at(enclosingEntity.symbol.owner, containingFile).report(
                POSSIBLE_INITIALIZATION_DEADLOCK,
                deadlockingEntities.map(EnclosingEntity<*>::name)
            )
        }
    }

    context(reporter: IrDiagnosticReporter, containingFile: IrFile)
    private fun DependencyGraphAnalyzer.checkObjectConstructor(enclosingEntity: EnclosingEntity.Object) =
        analyze(enclosingEntity.beginInitializationIndex).forEach { reportResultAndPossibleUninitialization(it) }

    context(reporter: IrDiagnosticReporter, containingFile: IrFile)
    private fun DependencyGraphAnalyzer.checkAccessesInInitializer(initializerNode: AnonymousInitializerIndex) =
        analyze(initializerNode).forEach { reportResultAndPossibleUninitialization(it) }

    context(reporter: IrDiagnosticReporter, containingFile: IrFile)
    private fun DependencyGraphAnalyzer.checkEnumEntry(enclosingEntity: EnclosingEntity.EnumEntry) {
        val isPossiblyUninitialized = analyze(enclosingEntity.beginInitializationIndex).fold(false) { isUninitialized, result ->
            isUninitialized || reportResultAndPossibleUninitialization(result)
        }
        if (isPossiblyUninitialized) {
            reporter.at(enclosingEntity.symbol.owner, containingFile).report(
                POSSIBLY_UNINITIALIZED_ENUM_ENTRY,
                enclosingEntity.symbol,
                mutuallyDependentEntities(enclosingEntity).mapTo(mutableListOf(), EnclosingEntity<*>::name)
            )
        }
    }

    context(reporter: IrDiagnosticReporter, containingFile: IrFile)
    fun DependencyGraphAnalyzer.checkProperty(propertyNode: PropertyIndex) {
        val isPossiblyUninitialized = analyze(propertyNode).fold(false) { isUninitialized, result ->
            isUninitialized || reportResultAndPossibleUninitialization(result)
        }
        if (isPossiblyUninitialized) {
            reporter.at(propertyNode.symbol.owner, containingFile).report(
                POSSIBLY_UNINITIALIZED_PROPERTY,
                propertyNode.name,
                propertyNode.enclosingEntity?.let {
                    mutuallyDependentEntities(it).mapTo(mutableListOf(), EnclosingEntity<*>::name)
                } ?: emptyList()
            )
        }
    }
}
