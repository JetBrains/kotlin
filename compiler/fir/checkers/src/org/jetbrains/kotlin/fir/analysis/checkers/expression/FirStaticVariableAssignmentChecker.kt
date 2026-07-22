/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.finalApproximationOrSelf
import org.jetbrains.kotlin.fir.analysis.checkers.toTypeInfo
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.allInheritors
import org.jetbrains.kotlin.fir.declarations.utils.isExtension
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.references.toResolvedEnumEntrySymbol
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.firOverrideChecker
import org.jetbrains.kotlin.fir.scopes.isOverriddenProperty
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.resolvedType

object FirStaticVariableAssignmentChecker : FirVariableAssignmentChecker(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirVariableAssignment) {
        val lValue = expression.lValue
        if (lValue !is FirPropertyAccessExpression) return
        val property = lValue.calleeReference.toResolvedPropertySymbol(discardErrorReference = true) ?: return
        // FIR has no invariants I swear to god
        if (!property.isVar) return
        // Consider only non-local and non-extensions vars (of course)
        if (property.isLocal || property.isExtension) return
        if (property.isStatic) {
            // Companion block members
            property.reportOn(expression)
            return
        }
        val receiver = lValue.explicitReceiver ?: lValue.extensionReceiver ?: lValue.dispatchReceiver
        when (receiver) {
            // Trivial case: the property is top-level (no receiver)
            null -> property.reportOn(expression)
            // Trivial case: direct object access
            is FirResolvedQualifier -> {
                val classSymbol = receiver.accessedObjectSymbol ?: return
                val actualProperty = property.findSingletonOverrideOrSelf(classSymbol)
                actualProperty.reportOn(expression)
            }
            // Trivial case: direct enum entry access
            is FirPropertyAccessExpression -> {
                val enumEntry = receiver.calleeReference.toResolvedEnumEntrySymbol(discardErrorReference = true) ?: run {
                    property.checkReceiverInheritors(receiver, expression)
                    return
                }
                val classSymbol = enumEntry.initializerObjectSymbol ?: enumEntry.getContainingClassSymbol() as? FirClassSymbol ?: return
                val actualProperty = property.findSingletonOverrideOrSelf(classSymbol)
                actualProperty.reportOn(expression)
            }
            // Non-trivial case: aliased access to a singleton, cannot resolve without an expensive interprocedural points-to analysis
            // - We can only approximate if such reassignment can possibly happen using the static type of the receiver expression
            else -> property.checkReceiverInheritors(receiver, expression)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun FirPropertySymbol.checkReceiverInheritors(receiver: FirExpression, assignment: FirVariableAssignment) {
        val receiverType = receiver.resolvedType.finalApproximationOrSelf().toClassSymbol() ?: return
        val [singletonSymbol, virtualReassignment] = when {
            // Catch resolved singleton types early
            receiverType.classKind.isSingleton -> receiverType to false
            else -> (receiverType as? FirRegularClassSymbol)?.allInheritors?.firstOrNull { it.classKind.isSingleton }?.let { it to true }
        } ?: return
        val actualProperty = findSingletonOverrideOrSelf(singletonSymbol)
        actualProperty.reportOn(assignment, virtualReassignment = virtualReassignment)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun FirPropertySymbol.reportOn(expression: FirVariableAssignment, virtualReassignment: Boolean = false): Unit = when {
        // Custom setters can cause initialization issues regardless of the type being assigned
        setterSymbol?.let { !it.isDefault } ?: false -> reporter.reportOn(
            expression.source,
            if (virtualReassignment) FirErrors.POSSIBLY_REASSIGNED_STATIC_VAR_WITH_SETTER else FirErrors.REASSIGNED_STATIC_VAR_WITH_SETTER
        )
        // Instanced vars can introduce dependencies due to dynamic dispatch
        !resolvedReturnType.toTypeInfo(context.session).isPrimitive -> reporter.reportOn(
            expression.source,
            if (virtualReassignment) FirErrors.POSSIBLY_REASSIGNED_INSTANCED_STATIC_VAR else FirErrors.REASSIGNED_INSTANCED_STATIC_VAR
        )
        else -> {}
    }

    context(context: CheckerContext)
    private fun FirPropertySymbol.findSingletonOverrideOrSelf(singletonSymbol: FirClassSymbol<*>): FirPropertySymbol {
        val useSiteScope = singletonSymbol.unsubstitutedScope()
        var overriddenProperty: FirPropertySymbol? = null
        useSiteScope.processPropertiesByName(name) { property ->
            if (property is FirPropertySymbol && context.session.firOverrideChecker.isOverriddenProperty(property, this)) {
                overriddenProperty = property
                ProcessorAction.STOP
            }
            ProcessorAction.NEXT
        }
        return overriddenProperty ?: this
    }
}
