/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.argumentMapping
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.originalOrSelf
import org.jetbrains.kotlin.fir.resolve.inference.inferenceComponents
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.typeConstructor
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

/**
 * Checks compatibility of variance of type argument for Java collections.
 *
 * Java collection interfaces all include methods mutating the collection. Hence, they naturally map to mutable versions of Kotlin
 * collections. But Kotlin provides immutable collections and it's good practice to enforce immutability when possible. Hence, to make it
 * easier to use immutable collections in Kotlin, Java collection types are instead mapped to flexible type with mutable collection as the
 * lower type and immutable collection as upper type. However, flexible types make type checking unsound. Hence we have this check that
 * catches a common mistake allowed by flexible type.
 *
 * Consider a Java method accepting type `List<Object>`, which is mapped to `MutableList<Any?>..List<(out) Any?>`. If one passes a mutable
 * list of some more concrete type than `Any` (ArrayList<String>, LinkedList<Int>, etc.) to this Java method, then any writes to this
 * mutable collection could cause `ClassCastException`s if the same mutable list is read elsewhere. It's the purpose of this checker to
 * reject such code.
 *
 * On the other hand, if one passes an immutable list of some more concrete type, then any writes to it on the Java side would cause
 * `UnsupportedOperationException`, which is expected and the price we pay in order to make immutable collection easier to use. This checker
 * doesn't do anything to prevent this from happening.
 */
object FirJavaGenericVarianceViolationTypeChecker : FirFunctionCallChecker() {
    private val javaOrigin = setOf(FirDeclarationOrigin.Java, FirDeclarationOrigin.Enhancement)

    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val calleeFunction = expression.calleeReference.toResolvedCallableSymbol()?.fir as? FirFunction<*> ?: return
        if (calleeFunction.originalOrSelf().origin !in javaOrigin) {
            return
        }
        val argumentMapping = expression.argumentMapping ?: return
        val typeArgumentMap = mutableMapOf<FirTypeParameterSymbol, ConeKotlinType>()
        for (i in 0 until expression.typeArguments.size) {
            val type = expression.typeArguments[i].safeAs<FirTypeProjectionWithVariance>()?.typeRef?.coneTypeSafe<ConeKotlinType>()
            if (type != null) {
                typeArgumentMap[calleeFunction.typeParameters[i].symbol] = type
            }
        }
        val typeParameterSubstitutor = substitutorByMap(typeArgumentMap, context.session)
        for ((arg, param) in argumentMapping) {
            val expectedType = typeParameterSubstitutor.substituteOrSelf(param.returnTypeRef.coneType)

            // optimization: if no arguments or flexibility, everything is OK
            if (expectedType !is ConeFlexibleType || expectedType.typeArguments.isEmpty()) continue

            // Anything is acceptable for raw types
            if (expectedType is ConeRawType) continue

            val lowerBound = expectedType.lowerBound
            val upperBound = expectedType.upperBound
            val typeCtx = context.session.inferenceComponents.ctx
            val lowerConstructor = lowerBound.typeConstructor(typeCtx)
            val upperConstructor = upperBound.typeConstructor(typeCtx)

            // Use site variance projection is always the same for flexible types. So there is no need to check if declaration site is the
            // same.
            if (lowerConstructor == upperConstructor) return

            val argType = arg.typeRef.coneType

            // If the base class of the argument type is not equal or a sub class of the lower bound, then we simply allow it so that
            // Kotlin immutable collections can be used in place of Java collection types.
            if (!typeCtx.isTypeConstructorEqualOrSubClassOf(argType, lowerBound)) return

            if (!AbstractTypeChecker.isSubtypeOf(typeCtx, argType, lowerBound.withNullability(ConeNullability.NULLABLE, typeCtx))) {
                reporter.reportOn(arg.source, FirErrors.JAVA_TYPE_MISMATCH, argType, expectedType, context)
            }
        }
    }

    private fun ConeInferenceContext.isTypeConstructorEqualOrSubClassOf(subType: ConeKotlinType, superType: ConeKotlinType): Boolean {
        return isTypeConstructorEqualOrSubClassOf(subType.typeConstructor(), superType.typeConstructor())
    }

    private fun ConeInferenceContext.isTypeConstructorEqualOrSubClassOf(
        subTypeConstructor: TypeConstructorMarker,
        superTypeConstructor: TypeConstructorMarker
    ): Boolean {
        if (subTypeConstructor == superTypeConstructor) return true
        for (immediateSuperType in subTypeConstructor.supertypes()) {
            val immediateSuperTypeConstructor = immediateSuperType.typeConstructor()
            if (superTypeConstructor == immediateSuperTypeConstructor) return true
            if (this@isTypeConstructorEqualOrSubClassOf.isTypeConstructorEqualOrSubClassOf(immediateSuperTypeConstructor, superTypeConstructor)) return true
        }
        return false
    }
}