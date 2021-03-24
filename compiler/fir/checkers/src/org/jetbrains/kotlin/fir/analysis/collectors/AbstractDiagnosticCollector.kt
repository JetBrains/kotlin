/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.PersistentCheckerContext
import org.jetbrains.kotlin.fir.analysis.collectors.components.*
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.SessionHolder
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
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

    @Suppress("LeakingThis")
    private val visitor = Visitor(PersistentCheckerContext(this, returnTypeCalculator))

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

    private inner class Visitor(context: PersistentCheckerContext) : AbstractDiagnosticCollectorVisitor(context) {
        private fun <T : FirElement> T.runComponents() {
            if (currentAction.checkInCurrentDeclaration) {
                beforeRunningAllComponentsOnElement(this)
                components.forEach {
                    beforeRunningSingleComponentOnElement(this)
                    this.accept(it, context)
                }
            }
        }

        override fun visitElement(element: FirElement, data: Any?) {
            if (element is FirAnnotationContainer) {
                visitAnnotationContainer(element, data)
                return
            }
            element.runComponents()
            element.acceptChildren(this, null)
        }

        override fun visitAnnotationContainer(annotationContainer: FirAnnotationContainer, data: Any?) {
            withSuppressedDiagnostics(annotationContainer) {
                annotationContainer.runComponents()
                annotationContainer.acceptChildren(this, null)
            }
        }

        override fun visitTypeAlias(typeAlias: FirTypeAlias, data: Any?) {
            withSuppressedDiagnostics(typeAlias) {
                visitWithDeclaration(typeAlias)
            }
        }

        private fun visitJump(loopJump: FirLoopJump) {
            withSuppressedDiagnostics(loopJump) {
                loopJump.runComponents()
                loopJump.target.labeledElement.takeIf { it is FirErrorLoop }?.accept(this, null)
            }
        }

        override fun visitBreakExpression(breakExpression: FirBreakExpression, data: Any?) {
            visitJump(breakExpression)
        }

        override fun visitContinueExpression(continueExpression: FirContinueExpression, data: Any?) {
            visitJump(continueExpression)
        }

        private fun visitClassAndChildren(klass: FirClass<*>, type: ConeKotlinType) {
            val typeRef = buildResolvedTypeRef {
                this.type = type
            }
            visitWithDeclarationAndReceiver(klass, (klass as? FirRegularClass)?.name, typeRef)
        }

        override fun visitRegularClass(regularClass: FirRegularClass, data: Any?) {
            withSuppressedDiagnostics(regularClass) {
                visitClassAndChildren(regularClass, regularClass.defaultType())
            }
        }

        override fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: Any?) {
            withSuppressedDiagnostics(anonymousObject) {
                visitClassAndChildren(anonymousObject, anonymousObject.defaultType())
            }
        }

        override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: Any?) {
            withSuppressedDiagnostics(simpleFunction) {
                visitWithDeclarationAndReceiver(simpleFunction, simpleFunction.name, simpleFunction.receiverTypeRef)
            }
        }

        override fun visitConstructor(constructor: FirConstructor, data: Any?) {
            withSuppressedDiagnostics(constructor) {
                visitWithDeclaration(constructor)
            }
        }

        override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: Any?) {
            withSuppressedDiagnostics(anonymousFunction) {
                val labelName = anonymousFunction.label?.name?.let { Name.identifier(it) }
                visitWithDeclarationAndReceiver(
                    anonymousFunction,
                    labelName,
                    anonymousFunction.receiverTypeRef
                )
            }
        }

        override fun visitProperty(property: FirProperty, data: Any?) {
            withSuppressedDiagnostics(property) {
                visitWithDeclaration(property)
            }
        }

        override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: Any?) {
            if (propertyAccessor !is FirDefaultPropertyAccessor) {
                val property = context.containingDeclarations.last() as FirProperty
                withSuppressedDiagnostics(propertyAccessor) {
                    visitWithDeclarationAndReceiver(propertyAccessor, property.name, property.receiverTypeRef)
                }
            }
        }

        override fun visitValueParameter(valueParameter: FirValueParameter, data: Any?) {
            withSuppressedDiagnostics(valueParameter) {
                visitWithDeclaration(valueParameter)
            }
        }

        override fun visitEnumEntry(enumEntry: FirEnumEntry, data: Any?) {
            withSuppressedDiagnostics(enumEntry) {
                visitWithDeclaration(enumEntry)
            }
        }

        override fun visitFile(file: FirFile, data: Any?) {
            withSuppressedDiagnostics(file) {
                visitWithDeclaration(file)
            }
        }

        override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer, data: Any?) {
            visitWithDeclaration(anonymousInitializer)
        }

        override fun visitBlock(block: FirBlock, data: Any?) {
            withSuppressedDiagnostics(block) {
                visitExpression(block, data)
            }
        }

        override fun visitTypeRef(typeRef: FirTypeRef, data: Any?) {
            if (typeRef.source != null && typeRef.source?.kind !is FirFakeSourceElementKind) {
                withSuppressedDiagnostics(typeRef) {
                    typeRef.runComponents()
                    typeRef.acceptChildren(this, data)
                }
            }
        }

        override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: Any?) {
            super.visitResolvedTypeRef(resolvedTypeRef, data)
            resolvedTypeRef.delegatedTypeRef?.accept(this, data)
        }

        override fun visitFunctionCall(functionCall: FirFunctionCall, data: Any?) {
            visitWithQualifiedAccess(functionCall)
        }

        override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression, data: Any?) {
            visitWithQualifiedAccess(qualifiedAccessExpression)
        }

        override fun visitGetClassCall(getClassCall: FirGetClassCall, data: Any?) {
            visitWithGetClassCall(getClassCall)
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
            return withQualifiedAccess(qualifiedAccess){
                qualifiedAccess.runComponents()
                qualifiedAccess.acceptChildren(this, null)
            }
        }

        private fun visitWithGetClassCall(getClassCall: FirGetClassCall) {
            return withGetClassCall(getClassCall) {
                getClassCall.runComponents()
                getClassCall.acceptChildren(this, null)
            }
        }
    }

    protected open fun getDeclarationActionOnDeclarationEnter(declaration: FirDeclaration): DiagnosticCollectorDeclarationAction =
        DiagnosticCollectorDeclarationAction.CHECK_IN_CURRENT_DECLARATION_AND_LOOKUP_FOR_NESTED

    protected open fun onDeclarationExit(declaration: FirDeclaration) {}

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
