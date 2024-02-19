/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirBackingField
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirContextReceiver
import org.jetbrains.kotlin.fir.declarations.FirDanglingModifierList
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirErrorPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.FirErrorProperty
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirReceiverParameter
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRefsOwner
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirErrorAnnotationCall
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.customAnnotations
import org.jetbrains.kotlin.fir.types.forEachType
import org.jetbrains.kotlin.fir.visitors.FirVisitor

/**
 * Visitor for annotations outside bodies.
 * Such annotations are processed in [ANNOTATION_ARGUMENTS][org.jetbrains.kotlin.fir.declarations.FirResolvePhase.ANNOTATION_ARGUMENTS]
 * phase.
 *
 * This visitor is not recursive and processes only the target declaration (without unrelated nested declarations).
 * See [RecursiveNonLocalAnnotationVisitor] for the recursive visitor.
 *
 * @see processAnnotation
 * @see RecursiveNonLocalAnnotationVisitor
 */
internal abstract class NonLocalAnnotationVisitor<T> : FirVisitor<Unit, T>() {
    abstract fun processAnnotation(annotation: FirAnnotation, data: T)

    /**
     * Skip all [FirElementWithResolveState] without explicit override
     */
    override fun visitElement(element: FirElement, data: T) {
        if (element is FirElementWithResolveState) return

        element.acceptChildren(this, data)
    }

    /**
     * Skip argument list as the compiler do not support annotations inside annotation arguments
     */
    override fun visitArgumentList(argumentList: FirArgumentList, data: T) {}
    override fun visitAnnotationArgumentMapping(annotationArgumentMapping: FirAnnotationArgumentMapping, data: T) {}

    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: T) {
        visitTypeAnnotations(resolvedTypeRef, data)
        resolvedTypeRef.acceptChildren(this, data)
    }

    override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef, data: T) {
        visitResolvedTypeRef(errorTypeRef, data)
    }

    private fun visitTypeAnnotations(resolvedTypeRef: FirResolvedTypeRef, data: T) {
        resolvedTypeRef.coneType.forEachType { coneType ->
            for (typeArgumentAnnotation in coneType.customAnnotations) {
                typeArgumentAnnotation.accept(this, data)
            }
        }
    }

    override fun visitAnnotation(annotation: FirAnnotation, data: T) {
        processAnnotation(annotation, data)
        annotation.acceptChildren(this, data)
    }

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: T) {
        visitAnnotation(annotationCall, data)
    }

    override fun visitErrorAnnotationCall(errorAnnotationCall: FirErrorAnnotationCall, data: T) {
        visitAnnotation(errorAnnotationCall, data)
    }

    override fun visitFile(file: FirFile, data: T) {
        visitAnnotationContainer(file, data)
    }

    override fun visitDanglingModifierList(danglingModifierList: FirDanglingModifierList, data: T) {
        visitAnnotationContainer(danglingModifierList, data)
    }

    override fun visitScript(script: FirScript, data: T) {
        visitAnnotationContainer(script, data)
    }

    override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer, data: T) {
        visitAnnotationContainer(anonymousInitializer, data)
    }

    override fun visitMemberDeclaration(memberDeclaration: FirMemberDeclaration, data: T) {
        visitTypeParameterRefsOwner(memberDeclaration, data)
        visitAnnotationContainer(memberDeclaration, data)
    }

    override fun visitTypeAlias(typeAlias: FirTypeAlias, data: T) {
        visitMemberDeclaration(typeAlias, data)
        typeAlias.expandedTypeRef.accept(this, data)
    }

    override fun visitRegularClass(regularClass: FirRegularClass, data: T) {
        visitMemberDeclaration(regularClass, data)
        visitContextReceivers(regularClass.contextReceivers, data)
        regularClass.superTypeRefs.forEach { it.accept(this, data) }
    }

    override fun visitCallableDeclaration(callableDeclaration: FirCallableDeclaration, data: T) {
        visitMemberDeclaration(callableDeclaration, data)
        visitContextReceivers(callableDeclaration.contextReceivers, data)
        callableDeclaration.receiverParameter?.accept(this, data)
        callableDeclaration.returnTypeRef.accept(this, data)
    }

    fun visitContextReceivers(contextReceivers: List<FirContextReceiver>, data: T) {
        contextReceivers.forEach { it.accept(this, data) }
    }

    override fun visitAnnotationContainer(annotationContainer: FirAnnotationContainer, data: T) {
        annotationContainer.annotations.forEach { it.accept(this, data) }
    }

    override fun visitTypeParameterRefsOwner(typeParameterRefsOwner: FirTypeParameterRefsOwner, data: T) {
        typeParameterRefsOwner.typeParameters.forEach { it.accept(this, data) }
    }

    override fun visitReceiverParameter(receiverParameter: FirReceiverParameter, data: T) {
        receiverParameter.acceptChildren(this, data)
    }

    override fun visitContextReceiver(contextReceiver: FirContextReceiver, data: T) {
        contextReceiver.acceptChildren(this, data)
    }

    override fun visitTypeParameter(typeParameter: FirTypeParameter, data: T) {
        typeParameter.acceptChildren(this, data)
    }

    override fun visitFunction(function: FirFunction, data: T) {
        visitCallableDeclaration(function, data)
        function.valueParameters.forEach { it.accept(this, data) }
    }

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: T) {
        visitFunction(simpleFunction, data)
    }

    override fun visitConstructor(constructor: FirConstructor, data: T) {
        visitFunction(constructor, data)
    }

    override fun visitErrorPrimaryConstructor(errorPrimaryConstructor: FirErrorPrimaryConstructor, data: T) {
        visitFunction(errorPrimaryConstructor, data)
    }

    override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: T) {
        visitFunction(propertyAccessor, data)
    }

    override fun visitVariable(variable: FirVariable, data: T) {
        visitCallableDeclaration(variable, data)
        variable.getter?.accept(this, data)
        variable.setter?.accept(this, data)
        variable.backingField?.accept(this, data)
    }

    override fun visitEnumEntry(enumEntry: FirEnumEntry, data: T) {
        visitVariable(enumEntry, data)
    }

    override fun visitProperty(property: FirProperty, data: T) {
        visitVariable(property, data)
    }

    override fun visitErrorProperty(errorProperty: FirErrorProperty, data: T) {
        visitVariable(errorProperty, data)
    }

    override fun visitValueParameter(valueParameter: FirValueParameter, data: T) {
        visitVariable(valueParameter, data)
    }

    override fun visitBackingField(backingField: FirBackingField, data: T) {
        visitVariable(backingField, data)
    }
}