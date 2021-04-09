/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.PrivateForInline
import org.jetbrains.kotlin.fir.analysis.checkers.context.PersistentCheckerContext
import org.jetbrains.kotlin.fir.analysis.collectors.components.AbstractDiagnosticCollectorComponent
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.collectImplicitReceivers
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.name.Name

abstract class AbstractDiagnosticCollectorVisitor(
    @set:PrivateForInline var context: PersistentCheckerContext,
    protected val components: List<AbstractDiagnosticCollectorComponent>,
) : FirDefaultVisitor<Unit, Nothing?>() {

    protected open fun beforeRunningAllComponentsOnElement(element: FirElement) {}
    protected open fun beforeRunningSingleComponentOnElement(element: FirElement) {}

    protected open fun shouldVisitDeclaration(declaration: FirDeclaration) = true
    protected open fun onDeclarationExit(declaration: FirDeclaration) {}

    protected abstract fun goToNestedDeclarations(element: FirElement)
    protected abstract fun runComponents(element: FirElement)

    override fun visitElement(element: FirElement, data: Nothing?) {
        if (element is FirAnnotationContainer) {
            visitAnnotationContainer(element, data)
            return
        }
        runComponents(element)
        goToNestedDeclarations(element)
    }

    override fun visitAnnotationContainer(annotationContainer: FirAnnotationContainer, data: Nothing?) {
        withSuppressedDiagnostics(annotationContainer) {
            runComponents(annotationContainer)
            goToNestedDeclarations(annotationContainer)
        }
    }

    private fun visitJump(loopJump: FirLoopJump) {
        withSuppressedDiagnostics(loopJump) {
            runComponents(loopJump)
            loopJump.target.labeledElement.takeIf { it is FirErrorLoop }?.accept(this, null)
        }
    }

    override fun visitBreakExpression(breakExpression: FirBreakExpression, data: Nothing?) {
        visitJump(breakExpression)
    }

    override fun visitContinueExpression(continueExpression: FirContinueExpression, data: Nothing?) {
        visitJump(continueExpression)
    }

    private fun visitClassAndChildren(klass: FirClass<*>, type: ConeKotlinType) {
        val typeRef = buildResolvedTypeRef {
            this.type = type
        }
        visitWithDeclarationAndReceiver(klass, (klass as? FirRegularClass)?.name, typeRef)
    }

    override fun visitRegularClass(regularClass: FirRegularClass, data: Nothing?) {
        withSuppressedDiagnostics(regularClass) {
            visitClassAndChildren(regularClass, regularClass.defaultType())
        }
    }

    override fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: Nothing?) {
        withSuppressedDiagnostics(anonymousObject) {
            visitClassAndChildren(anonymousObject, anonymousObject.defaultType())
        }
    }

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: Nothing?) {
        withSuppressedDiagnostics(simpleFunction) {
            visitWithDeclarationAndReceiver(simpleFunction, simpleFunction.name, simpleFunction.receiverTypeRef)
        }
    }

    override fun visitConstructor(constructor: FirConstructor, data: Nothing?) {
        withSuppressedDiagnostics(constructor) {
            visitWithDeclaration(constructor)
        }
    }

    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: Nothing?) {
        withSuppressedDiagnostics(anonymousFunction) {
            val labelName = anonymousFunction.label?.name?.let { Name.identifier(it) }
            visitWithDeclarationAndReceiver(
                anonymousFunction,
                labelName,
                anonymousFunction.receiverTypeRef
            )
        }
    }

    override fun visitProperty(property: FirProperty, data: Nothing?) {
        withSuppressedDiagnostics(property) {
            visitWithDeclaration(property)
        }
    }

    override fun visitTypeAlias(typeAlias: FirTypeAlias, data: Nothing?) {
        withSuppressedDiagnostics(typeAlias) {
            visitWithDeclaration(typeAlias)
        }
    }

    override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: Nothing?) {
        if (propertyAccessor !is FirDefaultPropertyAccessor) {
            val property = context.containingDeclarations.last() as FirProperty
            withSuppressedDiagnostics(propertyAccessor) {
                visitWithDeclarationAndReceiver(propertyAccessor, property.name, property.receiverTypeRef)
            }
        }
    }

    override fun visitValueParameter(valueParameter: FirValueParameter, data: Nothing?) {
        withSuppressedDiagnostics(valueParameter) {
            visitWithDeclaration(valueParameter)
        }
    }

    override fun visitEnumEntry(enumEntry: FirEnumEntry, data: Nothing?) {
        withSuppressedDiagnostics(enumEntry) {
            visitWithDeclaration(enumEntry)
        }
    }

    override fun visitFile(file: FirFile, data: Nothing?) {
        withSuppressedDiagnostics(file) {
            visitWithDeclaration(file)
        }
    }

    override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer, data: Nothing?) {
        visitWithDeclaration(anonymousInitializer)
    }

    override fun visitBlock(block: FirBlock, data: Nothing?) {
        withSuppressedDiagnostics(block) {
            visitExpression(block, data)
        }
    }

    override fun visitTypeRef(typeRef: FirTypeRef, data: Nothing?) {
        if (typeRef.source != null && typeRef.source?.kind !is FirFakeSourceElementKind) {
            withSuppressedDiagnostics(typeRef) {
                runComponents(typeRef)
                goToNestedDeclarations(typeRef)
            }
        }
    }

    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: Nothing?) {
        super.visitResolvedTypeRef(resolvedTypeRef, data)
        resolvedTypeRef.delegatedTypeRef?.accept(this, data)
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: Nothing?) {
        visitWithQualifiedAccess(functionCall)
    }

    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression, data: Nothing?) {
        visitWithQualifiedAccess(qualifiedAccessExpression)
    }

    override fun visitGetClassCall(getClassCall: FirGetClassCall, data: Nothing?) {
        visitWithGetClassCall(getClassCall)
    }

    protected inline fun visitWithDeclaration(
        declaration: FirDeclaration,
        block: () -> Unit = { goToNestedDeclarations(declaration) }
    ) {
        if (shouldVisitDeclaration(declaration)) {
            runComponents(declaration)
            withDeclaration(declaration) {
                block()
            }
            onDeclarationExit(declaration)
        }
    }

    private fun visitWithDeclarationAndReceiver(declaration: FirDeclaration, labelName: Name?, receiverTypeRef: FirTypeRef?) {
        visitWithDeclaration(declaration) {
            withLabelAndReceiverType(
                labelName,
                declaration,
                receiverTypeRef?.coneTypeSafe()
            ) {
                goToNestedDeclarations(declaration)
            }
        }
    }

    private fun visitWithQualifiedAccess(qualifiedAccess: FirQualifiedAccess) {
        return withQualifiedAccess(qualifiedAccess) {
            runComponents(qualifiedAccess)
            goToNestedDeclarations(qualifiedAccess)
        }
    }

    private fun visitWithGetClassCall(getClassCall: FirGetClassCall) {
        return withGetClassCall(getClassCall) {
            runComponents(getClassCall)
            goToNestedDeclarations(getClassCall)
        }
    }

    @OptIn(PrivateForInline::class)
    inline fun <R> withQualifiedAccess(qualifiedAccess: FirQualifiedAccess, block: () -> R): R {
        val existingContext = context
        context = context.addQualifiedAccess(qualifiedAccess)
        try {
            return block()
        } finally {
            context = existingContext
        }
    }


    @OptIn(PrivateForInline::class)
    inline fun <R> withGetClassCall(getClassCall: FirGetClassCall, block: () -> R): R {
        val existingContext = context
        context = context.addGetClassCall(getClassCall)
        try {
            return block()
        } finally {
            context = existingContext
        }
    }


    @OptIn(PrivateForInline::class)
    inline fun <R> withDeclaration(declaration: FirDeclaration, block: () -> R): R {
        val existingContext = context
        context = context.addDeclaration(declaration)
        try {
            return block()
        } finally {
            context = existingContext
        }
    }


    @OptIn(PrivateForInline::class)
    inline fun <R> withLabelAndReceiverType(
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
    inline fun <R> withSuppressedDiagnostics(annotationContainer: FirAnnotationContainer, block: () -> R): R {
        val existingContext = context
        addSuppressedDiagnosticsToContext(annotationContainer)
        return try {
            block()
        } finally {
            context = existingContext
        }
    }


    @OptIn(PrivateForInline::class)
    fun addSuppressedDiagnosticsToContext(annotationContainer: FirAnnotationContainer) {
        val arguments = AbstractDiagnosticCollector.getDiagnosticsSuppressedForContainer(annotationContainer) ?: return
        context = context.addSuppressedDiagnostics(
            arguments,
            allInfosSuppressed = AbstractDiagnosticCollector.SUPPRESS_ALL_INFOS in arguments,
            allWarningsSuppressed = AbstractDiagnosticCollector.SUPPRESS_ALL_WARNINGS in arguments,
            allErrorsSuppressed = AbstractDiagnosticCollector.SUPPRESS_ALL_ERRORS in arguments
        )
    }
}