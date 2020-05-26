/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.PersistentCheckerContext
import org.jetbrains.kotlin.fir.analysis.collectors.components.*
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirBreakExpression
import org.jetbrains.kotlin.fir.expressions.FirContinueExpression
import org.jetbrains.kotlin.fir.expressions.FirErrorLoop
import org.jetbrains.kotlin.fir.expressions.FirLoopJump
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.SessionHolder
import org.jetbrains.kotlin.fir.resolve.collectImplicitReceivers
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.Name

abstract class AbstractDiagnosticCollector(
    override val session: FirSession,
    override val scopeSession: ScopeSession = ScopeSession()
) : SessionHolder {
    fun collectDiagnostics(firFile: FirFile): Iterable<FirDiagnostic<*>> {
        if (!componentsInitialized) {
            throw IllegalStateException("Components are not initialized")
        }
        initializeCollector()
        firFile.accept(visitor)
        return getCollectedDiagnostics()
    }

    protected abstract fun initializeCollector()
    protected abstract fun getCollectedDiagnostics(): Iterable<FirDiagnostic<*>>
    abstract fun runCheck(block: (DiagnosticReporter) -> Unit)

    private val components: MutableList<AbstractDiagnosticCollectorComponent> = mutableListOf()
    private var componentsInitialized = false
    private val visitor = Visitor()

    @Suppress("LeakingThis")
    private var context = PersistentCheckerContext(this)

    fun initializeComponents(vararg components: AbstractDiagnosticCollectorComponent) {
        if (componentsInitialized) {
            throw IllegalStateException()
        }
        this.components += components
        componentsInitialized = true
    }

    private inner class Visitor : FirVisitorVoid() {
        private fun <T : FirElement> T.runComponents() {
            components.forEach {
                this.accept(it, context)
            }
        }

        override fun visitElement(element: FirElement) {
            element.runComponents()
            element.acceptChildren(this)
        }

        private fun visitJump(loopJump: FirLoopJump) {
            loopJump.runComponents()
            loopJump.acceptChildren(this)
            loopJump.target.labeledElement.takeIf { it is FirErrorLoop }?.accept(this)
        }

        override fun visitBreakExpression(breakExpression: FirBreakExpression) {
            visitJump(breakExpression)
        }

        override fun visitContinueExpression(continueExpression: FirContinueExpression) {
            visitJump(continueExpression)
        }

        private fun visitClassAndChildren(klass: FirClass<*>, type: ConeKotlinType) {
            val typeRef = buildResolvedTypeRef {
                this.type = type
            }
            visitWithDeclarationAndReceiver(klass, (klass as? FirRegularClass)?.name, typeRef)

        }

        override fun visitRegularClass(regularClass: FirRegularClass) {
            visitClassAndChildren(regularClass, regularClass.defaultType())
        }

        override fun visitAnonymousObject(anonymousObject: FirAnonymousObject) {
            visitClassAndChildren(anonymousObject, anonymousObject.defaultType())
        }

        override fun visitSimpleFunction(simpleFunction: FirSimpleFunction) {
            visitWithDeclarationAndReceiver(simpleFunction, simpleFunction.name, simpleFunction.receiverTypeRef)
        }

        override fun visitConstructor(constructor: FirConstructor) {
            visitWithDeclaration(constructor)
        }

        override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction) {
            val labelName = anonymousFunction.label?.name?.let { Name.identifier(it) }
            visitWithDeclarationAndReceiver(
                anonymousFunction,
                labelName,
                anonymousFunction.receiverTypeRef
            )
        }

        override fun visitProperty(property: FirProperty) {
            visitWithDeclaration(property)
        }

        override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor) {
            val property = context.containingDeclarations.last() as FirProperty
            visitWithDeclarationAndReceiver(propertyAccessor, property.name, property.receiverTypeRef)
        }

        override fun visitValueParameter(valueParameter: FirValueParameter) {
            visitWithDeclaration(valueParameter)
        }

        override fun visitEnumEntry(enumEntry: FirEnumEntry) {
            visitWithDeclaration(enumEntry)
        }

        override fun visitFile(file: FirFile) {
            visitWithDeclaration(file)
        }

        override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer) {
            visitWithDeclaration(anonymousInitializer)
        }

        private fun visitWithDeclaration(declaration: FirDeclaration) {
            declaration.runComponents()
            withDeclaration(declaration) {
                declaration.acceptChildren(this)
            }
        }

        private fun visitWithDeclarationAndReceiver(declaration: FirDeclaration, labelName: Name?, receiverTypeRef: FirTypeRef?) {
            declaration.runComponents()
            withDeclaration(declaration) {
                withLabelAndReceiverType(
                    labelName,
                    declaration,
                    receiverTypeRef?.coneTypeSafe()
                ) {
                    declaration.acceptChildren(this)
                }
            }
        }
    }

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
}

fun AbstractDiagnosticCollector.registerAllComponents() {
    initializeComponents(
        DeclarationCheckersDiagnosticComponent(this),
        ExpressionCheckersDiagnosticComponent(this),
        ErrorNodeDiagnosticCollectorComponent(this),
        ControlFlowAnalysisDiagnosticComponent(this),
    )
}