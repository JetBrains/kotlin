/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.scopes

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.enhancement.readOnlyToMutable
import org.jetbrains.kotlin.fir.java.toConeKotlinTypeProbablyFlexible
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.impl.FirAbstractOverrideChecker
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*

class JavaOverrideChecker internal constructor(
    private val session: FirSession,
    private val javaTypeParameterStack: JavaTypeParameterStack
) : FirAbstractOverrideChecker() {
    private val context: ConeTypeContext = session.typeContext

    private fun isEqualTypes(candidateType: ConeKotlinType, baseType: ConeKotlinType, substitutor: ConeSubstitutor): Boolean {
        if (candidateType is ConeFlexibleType) return isEqualTypes(candidateType.lowerBound, baseType, substitutor)
        if (baseType is ConeFlexibleType) return isEqualTypes(candidateType, baseType.lowerBound, substitutor)
        if (candidateType is ConeClassLikeType && baseType is ConeClassLikeType) {
            return candidateType.lookupTag.classId.let { it.readOnlyToMutable() ?: it } == baseType.lookupTag.classId.let { it.readOnlyToMutable() ?: it }
        }
        if (candidateType is ConeClassLikeType && baseType is ConeTypeParameterType) {
            val boundType = baseType.lookupTag.typeParameterSymbol.fir.bounds.singleOrNull()?.toConeKotlinTypeProbablyFlexible(
                session, javaTypeParameterStack
            )
            if (boundType != null) {
                return isEqualTypes(candidateType, boundType, substitutor)
            }
        }
        return with(context) {
            areEqualTypeConstructors(
                substitutor.substituteOrSelf(candidateType).typeConstructor(),
                substitutor.substituteOrSelf(baseType).typeConstructor()
            )
        }
    }

    override fun isEqualTypes(candidateTypeRef: FirTypeRef, baseTypeRef: FirTypeRef, substitutor: ConeSubstitutor) =
        isEqualTypes(
            candidateTypeRef.toConeKotlinTypeProbablyFlexible(session, javaTypeParameterStack),
            baseTypeRef.toConeKotlinTypeProbablyFlexible(session, javaTypeParameterStack),
            substitutor
        )

    override fun buildTypeParametersSubstitutorIfCompatible(
        overrideCandidate: FirCallableMemberDeclaration<*>,
        baseDeclaration: FirCallableMemberDeclaration<*>
    ): ConeSubstitutor? {

        if (overrideCandidate.typeParameters.isEmpty() && baseDeclaration.typeParameters.isEmpty()) return ConeSubstitutor.Empty

        val typeParametersErasure =
            (overrideCandidate.typeParameters + baseDeclaration.typeParameters).associate {
                val symbol = it.symbol
                val firstBound = symbol.fir.bounds.first() // Note that in Java type parameter typed arguments always erased to first bound
                symbol to firstBound.toConeKotlinTypeProbablyFlexible(session, javaTypeParameterStack)
            }
        return substitutorByMap(typeParametersErasure)
    }

    override fun isOverriddenFunction(overrideCandidate: FirSimpleFunction, baseDeclaration: FirSimpleFunction): Boolean {
        if (overrideCandidate.isStatic != baseDeclaration.isStatic) return false
        // NB: overrideCandidate is from Java and has no receiver
        val receiverTypeRef = baseDeclaration.receiverTypeRef
        val baseParameterTypes = listOfNotNull(receiverTypeRef) + baseDeclaration.valueParameters.map { it.returnTypeRef }

        if (overrideCandidate.valueParameters.size != baseParameterTypes.size) return false
        val substitutor = buildTypeParametersSubstitutorIfCompatible(overrideCandidate, baseDeclaration) ?: return false

        return overrideCandidate.valueParameters.zip(baseParameterTypes).all { (paramFromJava, baseType) ->
            isEqualTypes(paramFromJava.returnTypeRef, baseType, substitutor)
        }
    }

    override fun isOverriddenProperty(overrideCandidate: FirCallableMemberDeclaration<*>, baseDeclaration: FirProperty): Boolean {
        if (baseDeclaration.modality == Modality.FINAL) return false
        val receiverTypeRef = baseDeclaration.receiverTypeRef
        return when (overrideCandidate) {
            is FirSimpleFunction -> {
                if (receiverTypeRef == null) {
                    // TODO: setters
                    return overrideCandidate.valueParameters.isEmpty()
                } else {
                    if (overrideCandidate.valueParameters.size != 1) return false
                    return isEqualTypes(receiverTypeRef, overrideCandidate.valueParameters.single().returnTypeRef, ConeSubstitutor.Empty)
                }
            }
            is FirProperty -> {
                val overrideReceiverTypeRef = overrideCandidate.receiverTypeRef
                return when {
                    receiverTypeRef == null -> overrideReceiverTypeRef == null
                    overrideReceiverTypeRef == null -> false
                    else -> isEqualTypes(receiverTypeRef, overrideReceiverTypeRef, ConeSubstitutor.Empty)
                }
            }
            else -> false
        }
    }

}
