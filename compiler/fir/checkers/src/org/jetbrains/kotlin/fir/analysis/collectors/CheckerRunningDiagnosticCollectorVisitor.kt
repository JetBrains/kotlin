/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.PersistentCheckerContext
import org.jetbrains.kotlin.fir.analysis.collectors.components.AbstractDiagnosticCollectorComponent
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.name.Name

open class CheckerRunningDiagnosticCollectorVisitor(
    context: PersistentCheckerContext,
    private val components: List<AbstractDiagnosticCollectorComponent>
) : AbstractDiagnosticCollectorVisitor(context) {
    private var currentAction = DiagnosticCollectorDeclarationAction.CHECK_IN_CURRENT_DECLARATION_AND_LOOKUP_FOR_NESTED

    protected open fun beforeRunningAllComponentsOnElement(element: FirElement) {}
    protected open fun beforeRunningSingleComponentOnElement(element: FirElement) {}

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

    override fun visitGetClassCall(getClassCall: FirGetClassCall, data: Nothing?) {
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
        return withQualifiedAccess(qualifiedAccess) {
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
}
