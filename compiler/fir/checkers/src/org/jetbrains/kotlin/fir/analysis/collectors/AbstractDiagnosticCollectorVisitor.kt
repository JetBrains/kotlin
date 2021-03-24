/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.PrivateForInline
import org.jetbrains.kotlin.fir.analysis.checkers.context.PersistentCheckerContext
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.resolve.collectImplicitReceivers
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.name.Name

abstract class AbstractDiagnosticCollectorVisitor(
    @set:PrivateForInline var context: PersistentCheckerContext
) : FirDefaultVisitor<Unit, Nothing?>() {

    @OptIn(PrivateForInline::class)
    protected inline fun <R> withQualifiedAccess(qualifiedAccess: FirQualifiedAccess, block: () -> R): R {
        val existingContext = context
        context = context.addQualifiedAccess(qualifiedAccess)
        try {
            return block()
        } finally {
            context = existingContext
        }
    }


    @OptIn(PrivateForInline::class)
    protected inline fun <R> withGetClassCall(getClassCall: FirGetClassCall, block: () -> R): R {
        val existingContext = context
        context = context.addGetClassCall(getClassCall)
        try {
            return block()
        } finally {
            context = existingContext
        }
    }


    @OptIn(PrivateForInline::class)
    protected inline fun <R> withDeclaration(declaration: FirDeclaration, block: () -> R): R {
        val existingContext = context
        context = context.addDeclaration(declaration)
        try {
            return block()
        } finally {
            context = existingContext
        }
    }


    @OptIn(PrivateForInline::class)
    protected inline fun <R> withLabelAndReceiverType(
        labelName: Name?,
        owner: FirDeclaration,
        type: ConeKotlinType?,
        block: () -> R
    ): R {
        val (implicitReceiverValue, implicitCompanionValues) = context.sessionHolder.collectImplicitReceivers(type, owner)
        val existingContext = context
        implicitCompanionValues.forEach { value ->
            context = context.addImplicitReceiver(null, value)
        }
        implicitReceiverValue?.let {
            context = context.addImplicitReceiver(labelName, it)
        }
        try {
            return block()
        } finally {
            context = existingContext
        }
    }


    @OptIn(PrivateForInline::class)
    protected inline fun <R> withSuppressedDiagnostics(annotationContainer: FirAnnotationContainer, block: () -> R): R {
        val existingContext = context
        addSuppressedDiagnosticsToContext(annotationContainer)
        return try {
            block()
        } finally {
            context = existingContext
        }
    }


    @OptIn(PrivateForInline::class)
    protected fun addSuppressedDiagnosticsToContext(annotationContainer: FirAnnotationContainer) {
        val arguments = AbstractDiagnosticCollector.getDiagnosticsSuppressedForContainer(annotationContainer) ?: return
        context = context.addSuppressedDiagnostics(
            arguments,
            allInfosSuppressed = AbstractDiagnosticCollector.SUPPRESS_ALL_INFOS in arguments,
            allWarningsSuppressed = AbstractDiagnosticCollector.SUPPRESS_ALL_WARNINGS in arguments,
            allErrorsSuppressed = AbstractDiagnosticCollector.SUPPRESS_ALL_ERRORS in arguments
        )
    }
}