/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.collectors.components.AbstractDiagnosticCollectorComponent
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.SessionHolder
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.StandardClassIds

abstract class AbstractDiagnosticCollector(
    override val session: FirSession,
    override val scopeSession: ScopeSession = ScopeSession(),
    protected val createComponents: (DiagnosticReporter) -> List<AbstractDiagnosticCollectorComponent>,
) : SessionHolder {
    fun collectDiagnostics(firDeclaration: FirDeclaration<*>, reporter: DiagnosticReporter) {
        val components = createComponents(reporter)
        val visitor = createVisitor(components)
        firDeclaration.accept(visitor, null)
    }

    protected abstract fun createVisitor(components: List<AbstractDiagnosticCollectorComponent>): CheckerRunningDiagnosticCollectorVisitor

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
