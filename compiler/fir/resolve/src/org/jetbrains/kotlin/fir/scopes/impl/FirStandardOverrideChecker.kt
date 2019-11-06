/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeContext
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.types.AbstractStrictEqualityTypeChecker.strictEqualTypes

class FirStandardOverrideChecker(session: FirSession) : FirAbstractOverrideChecker() {

    private val context: ConeTypeContext = session.typeContext

    private fun isEqualTypes(candidateType: ConeKotlinType, baseType: ConeKotlinType, substitutor: ConeSubstitutor) =
        strictEqualTypes(context, substitutor.substituteOrSelf(candidateType), substitutor.substituteOrSelf(baseType))

    override fun isEqualTypes(candidateTypeRef: FirTypeRef, baseTypeRef: FirTypeRef, substitutor: ConeSubstitutor) =
        isEqualTypes((candidateTypeRef as FirResolvedTypeRef).type, (baseTypeRef as FirResolvedTypeRef).type, substitutor)

    private fun isEqualReceiverTypes(candidateTypeRef: FirTypeRef?, baseTypeRef: FirTypeRef?, substitutor: ConeSubstitutor): Boolean {
        return when {
            candidateTypeRef != null && baseTypeRef != null -> isEqualTypes(candidateTypeRef, baseTypeRef, substitutor)
            else -> candidateTypeRef == null && baseTypeRef == null
        }
    }

    override fun isOverriddenFunction(overrideCandidate: FirSimpleFunction, baseDeclaration: FirSimpleFunction): Boolean {
        if (overrideCandidate.valueParameters.size != baseDeclaration.valueParameters.size) return false

        val substitutor = getSubstitutorIfTypeParametersAreCompatible(overrideCandidate, baseDeclaration) ?: return false

        if (!isEqualReceiverTypes(overrideCandidate.receiverTypeRef, baseDeclaration.receiverTypeRef, substitutor)) return false

        return overrideCandidate.valueParameters.zip(baseDeclaration.valueParameters).all { (memberParam, selfParam) ->
            isEqualTypes(memberParam.returnTypeRef, selfParam.returnTypeRef, substitutor)
        }

    }

    override fun isOverriddenProperty(
        overrideCandidate: FirCallableMemberDeclaration<*>,
        baseDeclaration: FirProperty
    ): Boolean {
        // TODO: substitutor
        return overrideCandidate is FirProperty &&
                isEqualReceiverTypes(overrideCandidate.receiverTypeRef, baseDeclaration.receiverTypeRef, ConeSubstitutor.Empty)
    }
}