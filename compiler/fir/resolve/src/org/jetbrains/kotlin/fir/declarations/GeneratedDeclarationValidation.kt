/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirAnnotationResolveStatus
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid

fun FirElement.validate() {
    accept(FirGeneratedElementsValidator)
}

/*
 * TODO's:
 *  - add proper error messages to all `require`
 *  - add validation of declaration origin and resolve phase for all declarations
 */
object FirGeneratedElementsValidator : FirDefaultVisitorVoid() {
    override fun visitElement(element: FirElement) {
        element.acceptChildren(this)
    }

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall) {
        require(annotationCall.resolveStatus == FirAnnotationResolveStatus.Resolved)
        annotationCall.acceptChildren(this)
    }

    override fun visitRegularClass(regularClass: FirRegularClass) {
        regularClass.acceptChildren(this)
    }

    override fun visitArgumentList(argumentList: FirArgumentList) {
        require(argumentList is FirResolvedArgumentList)
        argumentList.acceptChildren(this)
    }

    override fun visitNamedReference(namedReference: FirNamedReference) {
        require(namedReference is FirResolvedNamedReference)
        namedReference.acceptChildren(this)
    }

    override fun visitTypeRef(typeRef: FirTypeRef) {
        require(typeRef is FirResolvedTypeRef)
        typeRef.acceptChildren(this)
    }

    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
        resolvedTypeRef.annotations.forEach { it.accept(this) }
    }

    override fun visitDeclarationStatus(declarationStatus: FirDeclarationStatus) {
        require(declarationStatus is FirResolvedDeclarationStatus)
    }

    override fun visitTypeParameterRef(typeParameterRef: FirTypeParameterRef) {
        typeParameterRef.symbol.fir.accept(this)
    }
}
