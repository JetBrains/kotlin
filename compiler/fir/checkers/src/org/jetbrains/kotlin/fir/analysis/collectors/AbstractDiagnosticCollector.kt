/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.collectors.components.*
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.SessionHolder
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.fir.types.*

abstract class AbstractDiagnosticCollector(
    override val session: FirSession,
    override val scopeSession: ScopeSession = ScopeSession(),
) : SessionHolder {
    fun collectDiagnostics(firDeclaration: FirDeclaration): List<FirDiagnostic<*>> {
        if (!componentsInitialized) {
            throw IllegalStateException("Components are not initialized")
        }
        initializeCollector()
        firDeclaration.accept(visitor, null)
        return getCollectedDiagnostics()
    }

    protected abstract fun initializeCollector()
    protected abstract fun getCollectedDiagnostics(): List<FirDiagnostic<*>>
    abstract val reporter: DiagnosticReporter

    protected val components: MutableList<AbstractDiagnosticCollectorComponent> = mutableListOf()
    private var componentsInitialized = false

    protected abstract val visitor: CheckerRunningDiagnosticCollectorVisitor

    fun initializeComponents(vararg components: AbstractDiagnosticCollectorComponent) {
        if (componentsInitialized) {
            throw IllegalStateException()
        }
        this.components += components
        componentsInitialized = true
    }


    companion object {
        const val SUPPRESS_ALL_INFOS = "infos"
        const val SUPPRESS_ALL_WARNINGS = "warnings"
        const val SUPPRESS_ALL_ERRORS = "errors"

        fun getDiagnosticsSuppressedForContainer(annotationContainer: FirAnnotationContainer): List<String>? {
            val annotations = annotationContainer.annotations.filter {
                val type = it.annotationTypeRef.coneType as? ConeClassLikeType ?: return@filter false
                type.lookupTag.classId == StandardClassIds.Suppress
            }
            if (annotations.isEmpty()) return null
            return annotations.flatMap { annotationCall ->
                annotationCall.arguments.filterIsInstance<FirVarargArgumentsExpression>().flatMap { varargArgument ->
                    varargArgument.arguments.mapNotNull { (it as? FirConstExpression<*>)?.value as? String? }
                }
            }
        }
    }
}

enum class DiagnosticCollectorDeclarationAction(val checkInCurrentDeclaration: Boolean, val lookupForNestedDeclaration: Boolean) {
    CHECK_IN_CURRENT_DECLARATION_AND_LOOKUP_FOR_NESTED(checkInCurrentDeclaration = true, lookupForNestedDeclaration = true),
    CHECK_IN_CURRENT_DECLARATION_AND_DO_NOT_LOOKUP_FOR_NESTED(checkInCurrentDeclaration = true, lookupForNestedDeclaration = false),
    DO_NOT_CHECK_IN_CURRENT_DECLARATION_AND_LOOKUP_FOR_NESTED(checkInCurrentDeclaration = false, lookupForNestedDeclaration = true),
    SKIP(checkInCurrentDeclaration = false, lookupForNestedDeclaration = false),
}

fun AbstractDiagnosticCollector.registerAllComponents() {
    initializeComponents(
        DeclarationCheckersDiagnosticComponent(this),
        ExpressionCheckersDiagnosticComponent(this),
        ErrorNodeDiagnosticCollectorComponent(this),
        ControlFlowAnalysisDiagnosticComponent(this),
    )
}
