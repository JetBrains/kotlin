/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.PersistentCheckerContext
import org.jetbrains.kotlin.fir.analysis.collectors.components.*
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.SessionHolder
import org.jetbrains.kotlin.fir.resolve.collectImplicitReceivers
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.name.Name

abstract class AbstractDiagnosticCollector(
    override val session: FirSession,
    override val scopeSession: ScopeSession = ScopeSession(),
    returnTypeCalculator: ReturnTypeCalculator = ReturnTypeCalculatorForFullBodyResolve()
) : SessionHolder {
    fun collectDiagnostics(firFile: FirFile): List<FirDiagnostic<*>> {
        if (!componentsInitialized) {
            throw IllegalStateException("Components are not initialized")
        }
        initializeCollector()
        firFile.accept(visitor, null)
        return getCollectedDiagnostics()
    }

    protected abstract fun initializeCollector()
    protected abstract fun getCollectedDiagnostics(): List<FirDiagnostic<*>>
    abstract val reporter: DiagnosticReporter

    private val components: MutableList<AbstractDiagnosticCollectorComponent> = mutableListOf()
    private var componentsInitialized = false
    private val visitor = Visitor()

    @Suppress("LeakingThis")
    private var context = PersistentCheckerContext(this, returnTypeCalculator)
    private var currentAction = DiagnosticCollectorDeclarationAction.CHECK_IN_CURRENT_DECLARATION_AND_LOOKUP_FOR_NESTED

    fun initializeComponents(vararg components: AbstractDiagnosticCollectorComponent) {
        if (componentsInitialized) {
            throw IllegalStateException()
        }
        this.components += components
        componentsInitialized = true
    }

    protected open fun beforeRunningAllComponentsOnElement(element: FirElement) {}
    protected open fun beforeRunningSingleComponentOnElement(element: FirElement) {}

    private inner class Visitor : FirDefaultVisitor<Unit, Nothing?>() {
        private fun <T : FirElement> T.runComponents() {
            if (currentAction.checkInCurrentDeclaration) {
                beforeRunningAllComponentsOnElement(this)
                components.forEach {
                    beforeRunningSingleComponentOnElement(this)
                    this.accept(it, context)
                }
            }
        }

        override fun visitElement(element: FirElement, data: Nothing?) {
            if (element is FirAnnotationContainer) {
                visitAnnotationContainer(element, data)
                return
            }
            element.runComponents()
            element.acceptChildren(this, null)
        }

        override fun visitAnnotationContainer(annotationContainer: FirAnnotationContainer, data: Nothing?) {
            withSuppressedDiagnostics(annotationContainer) {
                annotationContainer.runComponents()
                annotationContainer.acceptChildren(this, null)
            }
        }

        private fun visitJump(loopJump: FirLoopJump) {
            withSuppressedDiagnostics(loopJump) {
                loopJump.runComponents()
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
                    typeRef.runComponents()
                    typeRef.acceptChildren(this, data)
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

        private inline fun visitWithDeclaration(
            declaration: FirDeclaration,
            block: () -> Unit = { declaration.acceptChildren(this, null) }
        ) {
            if (!currentAction.lookupForNestedDeclaration) return

            val action = getDeclarationActionOnDeclarationEnter(declaration)
            withDiagnosticsAction(action) {
                declaration.runComponents()
                withDeclaration(declaration) {
                    block()
                }
            }
            onDeclarationExit(declaration)
        }

        private fun visitWithDeclarationAndReceiver(declaration: FirDeclaration, labelName: Name?, receiverTypeRef: FirTypeRef?) {
            visitWithDeclaration(declaration) {
                withLabelAndReceiverType(
                    labelName,
                    declaration,
                    receiverTypeRef?.coneTypeSafe()
                ) {
                    declaration.acceptChildren(this, null)
                }
            }
        }

        private fun visitWithQualifiedAccess(qualifiedAccess: FirQualifiedAccess) {
            val existingContext = context
            context = context.addQualifiedAccess(qualifiedAccess)
            try {
                qualifiedAccess.runComponents()
                qualifiedAccess.acceptChildren(this, null)
            } finally {
                context = existingContext
            }
        }
    }

    protected open fun getDeclarationActionOnDeclarationEnter(declaration: FirDeclaration): DiagnosticCollectorDeclarationAction =
        DiagnosticCollectorDeclarationAction.CHECK_IN_CURRENT_DECLARATION_AND_LOOKUP_FOR_NESTED

    protected open fun onDeclarationExit(declaration: FirDeclaration) {}

    private inline fun <R> withDeclaration(declaration: FirDeclaration, block: () -> R): R {
        val existingContext = context
        context = context.addDeclaration(declaration)
        try {
            return block()
        } finally {
            context = existingContext
        }
    }

    private inline fun <R> withLabelAndReceiverType(
        labelName: Name?,
        owner: FirDeclaration,
        type: ConeKotlinType?,
        block: () -> R
    ): R {
        val (implicitReceiverValue, implicitCompanionValues) = collectImplicitReceivers(type, owner)
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

    private inline fun <R> withSuppressedDiagnostics(annotationContainer: FirAnnotationContainer, block: () -> R): R {
        val existingContext = context
        addSuppressedDiagnosticsToContext(annotationContainer)
        return try {
            block()
        } finally {
            context = existingContext
        }
    }

    private fun addSuppressedDiagnosticsToContext(annotationContainer: FirAnnotationContainer) {
        val arguments = getDiagnosticsSuppressedForContainer(annotationContainer) ?: return
        context = context.addSuppressedDiagnostics(
            arguments,
            allInfosSuppressed = SUPPRESS_ALL_INFOS in arguments,
            allWarningsSuppressed = SUPPRESS_ALL_WARNINGS in arguments,
            allErrorsSuppressed = SUPPRESS_ALL_ERRORS in arguments
        )
    }

    private inline fun <R> withDiagnosticsAction(action: DiagnosticCollectorDeclarationAction, block: () -> R): R {
        val oldAction = currentAction
        currentAction = action
        return try {
            block()
        } finally {
            currentAction = oldAction
        }
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
