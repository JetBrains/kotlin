/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirEmptyArgumentList
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor

fun FirElement.validate() {
    accept(FirGeneratedElementsValidator, null)
}

/*
 * TODO's:
 *  - add proper error messages to all `require`
 *  - add validation of declaration origin and resolve phase for all declarations
 */
object FirGeneratedElementsValidator : FirDefaultVisitor<Unit, Any?>() {
    override fun visitElement(element: FirElement, data: Any?) {
        element.acceptChildren(this, null)
    }

    override fun visitAnnotation(annotation: FirAnnotation, data: Any?) {
        annotation.acceptChildren(this, null)
    }

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: Any?) {
        annotationCall.acceptChildren(this, null)
    }

    override fun visitRegularClass(regularClass: FirRegularClass, data: Any?) {
        regularClass.acceptChildren(this, null)
    }

    override fun visitArgumentList(argumentList: FirArgumentList, data: Any?) {
        require(argumentList is FirResolvedArgumentList || argumentList is FirEmptyArgumentList)
        argumentList.acceptChildren(this, null)
    }

    override fun visitNamedReference(namedReference: FirNamedReference, data: Any?) {
        require(namedReference is FirResolvedNamedReference)
        namedReference.acceptChildren(this, null)
    }

    override fun visitTypeRef(typeRef: FirTypeRef, data: Any?) {
        typeRef.acceptChildren(this, null)
    }

    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: Any?) {
        resolvedTypeRef.annotations.forEach { it.accept(this, null) }
    }

    override fun visitDeclarationStatus(declarationStatus: FirDeclarationStatus, data: Any?) {
        require(declarationStatus is FirResolvedDeclarationStatus)
    }

    override fun visitTypeParameterRef(typeParameterRef: FirTypeParameterRef, data: Any?) {
        typeParameterRef.symbol.fir.accept(this, null)
    }
}
