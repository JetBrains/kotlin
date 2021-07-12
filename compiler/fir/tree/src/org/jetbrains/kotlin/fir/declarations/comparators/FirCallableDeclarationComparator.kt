/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.comparators

import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.types.FirTypeRefComparator

object FirCallableDeclarationComparator : Comparator<FirCallableDeclaration> {
    override fun compare(a: FirCallableDeclaration, b: FirCallableDeclaration): Int {
        val typeAndNameDiff = FirMemberDeclarationComparator.TypeAndNameComparator.compare(a, b)
        if (typeAndNameDiff != 0) {
            return typeAndNameDiff
        }

        // Compare the receiver type if any.
        if (a.receiverTypeRef != null || b.receiverTypeRef != null) {
            val aHasReceiverType = if (a.receiverTypeRef != null) 1 else 0
            val bHasReceiverType = if (b.receiverTypeRef != null) 1 else 0
            val receiverTypePresenceDiff = aHasReceiverType - bHasReceiverType
            if (receiverTypePresenceDiff != 0) {
                return receiverTypePresenceDiff
            }
            assert(a.receiverTypeRef != null && b.receiverTypeRef != null)
        }

        // Compare the return type.
        val returnTypeDiff = FirTypeRefComparator.compare(a.returnTypeRef, b.returnTypeRef)
        if (returnTypeDiff != 0) {
            return returnTypeDiff
        }

        // Compare the value parameters for functions.
        if (a is FirFunction) {
            require(b is FirFunction) {
                "TypeAndNameComparator is inconsistent: ${a.render()} v.s. ${b.render()}"
            }
            val valueParameterSizeDiff = a.valueParameters.size - b.valueParameters.size
            if (valueParameterSizeDiff != 0) {
                return valueParameterSizeDiff
            }
            for ((aValueParameter, bValueParameter) in a.valueParameters.zip(b.valueParameters)) {
                val valueParameterDiff = FirValueParameterComparator.compare(aValueParameter, bValueParameter)
                if (valueParameterDiff != 0) {
                    return valueParameterDiff
                }
            }
        }

        // Compare the type parameters.
        val typeParameterSizeDiff = a.typeParameters.size - b.typeParameters.size
        if (typeParameterSizeDiff != 0) {
            return typeParameterSizeDiff
        }
        for ((aTypeParameter, bTypeParameter) in a.typeParameters.zip(b.typeParameters)) {
            val typeParameterDiff = FirTypeParameterRefComparator.compare(aTypeParameter, bTypeParameter)
            if (typeParameterDiff != 0) {
                return typeParameterDiff
            }
        }

        // Lastly, compare the fully qualified package name.
        return a.symbol.callableId.packageName.asString().compareTo(b.symbol.callableId.packageName.asString())
    }
}
