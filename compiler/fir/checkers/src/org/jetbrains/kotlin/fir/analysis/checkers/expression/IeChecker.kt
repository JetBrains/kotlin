/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.expressions.argument
import org.jetbrains.kotlin.fir.resolve.createSubstitutionForSupertype
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isNullableAny
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.type

/**
 * Reports [FirErrors.IE_DIAGNOSTIC] on suspicious downcasts.
 *
 * A downcast `subject as/is Target` is considered suspicious if the subject type has a type argument that is:
 * - not generalized: the argument is a ground type (e.g. `Int`) or a type parameter with more restrictive
 *   constraints than the corresponding parameter of the subject's class, and
 * - not inherited: the corresponding type parameter of the subject's class is not directly passed through
 *   (with exactly the same variance and bounds) along every step of the inheritance path from the target class.
 */
object IeChecker : FirTypeOperatorCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirTypeOperatorCall) {
        when (expression.operation) {
            FirOperation.IS, FirOperation.NOT_IS, FirOperation.AS, FirOperation.SAFE_AS -> {}
            else -> return
        }

        val session = context.session
        val subjectType = expression.argument.resolvedType.fullyExpandedType() as? ConeClassLikeType ?: return
        val targetType = expression.conversionTypeRef.coneType.fullyExpandedType() as? ConeClassLikeType ?: return

        val subjectClass = subjectType.toRegularClassSymbol(session) ?: return
        val targetClass = targetType.toRegularClassSymbol(session) ?: return

        if (subjectClass.typeParameterSymbols.isEmpty()) return
        if (!isDowncast(subjectClass, targetClass, session)) return

        val inheritedIndices = collectInheritedParameterIndices(targetClass, subjectClass, session)
        val subjectSubstitutor = subjectType.substitutorForOwnParameters(subjectClass, session)

        for (index in subjectClass.typeParameterSymbols.indices) {
            val parameter = subjectClass.typeParameterSymbols[index]
            val argument = subjectType.typeArguments.getOrNull(index) ?: continue
            if (index in inheritedIndices) continue
            if (isGeneralized(argument, parameter, subjectSubstitutor)) continue
            reporter.reportOn(
                expression.source,
                FirErrors.IE_DIAGNOSTIC,
                "suspicious downcast: type argument '$argument' for parameter '${parameter.name}' of '${subjectClass.classId}' " +
                        "is neither generalized nor inherited by '${targetClass.classId}'"
            )
        }
    }

    /**
     * A cast is a downcast if the target class is a strict subclass of the subject class.
     */
    private fun isDowncast(subjectClass: FirRegularClassSymbol, targetClass: FirRegularClassSymbol, session: FirSession): Boolean {
        return subjectClass != targetClass && targetClass.hasSuperClass(subjectClass, session)
    }

    private fun FirRegularClassSymbol.hasSuperClass(other: FirRegularClassSymbol, session: FirSession): Boolean {
        return resolvedSuperTypes.any { superType ->
            val superClass = superType.fullyExpandedType(session).toRegularClassSymbol(session) ?: return@any false
            superClass == other || superClass.hasSuperClass(other, session)
        }
    }

    /**
     * Computes the set of [subjectClass] type parameter indices that are inherited by [targetClass]:
     * on every step of every inheritance path from [targetClass] to [subjectClass] the parameter is directly
     * passed as a plain type parameter reference with exactly the same variance and bounds.
     */
    private fun collectInheritedParameterIndices(
        targetClass: FirRegularClassSymbol,
        subjectClass: FirRegularClassSymbol,
        session: FirSession,
    ): Set<Int> = inheritanceMapping(targetClass, subjectClass, session, mutableMapOf())?.keys ?: emptySet()

    /**
     * Returns a mapping from inherited [subjectClass] parameter indices to the corresponding parameter indices
     * of [klass], or `null` if [klass] does not reach [subjectClass] at all.
     */
    private fun inheritanceMapping(
        klass: FirRegularClassSymbol,
        subjectClass: FirRegularClassSymbol,
        session: FirSession,
        cache: MutableMap<FirRegularClassSymbol, Map<Int, Int>?>,
    ): Map<Int, Int>? {
        if (klass == subjectClass) {
            return klass.typeParameterSymbols.indices.associateWith { it }
        }
        cache[klass]?.let { return it }

        val mappingsThroughSupertypes = klass.resolvedSuperTypes.mapNotNull { superType ->
            val expandedSuperType = superType.fullyExpandedType(session) as? ConeClassLikeType ?: return@mapNotNull null
            val superClass = expandedSuperType.toRegularClassSymbol(session) ?: return@mapNotNull null
            val superMapping = inheritanceMapping(superClass, subjectClass, session, cache) ?: return@mapNotNull null
            mappingThroughSupertype(klass, expandedSuperType, superClass, superMapping, session)
        }

        val result = when {
            mappingsThroughSupertypes.isEmpty() -> null
            else -> mappingsThroughSupertypes.reduce(::intersectMappings)
        }
        cache[klass] = result
        return result
    }

    /**
     * Given a mapping of inherited subject parameters into [superClass] parameters, computes which of them
     * are still inherited in [klass] through the immediate supertype application [superType].
     */
    private fun mappingThroughSupertype(
        klass: FirRegularClassSymbol,
        superType: ConeClassLikeType,
        superClass: FirRegularClassSymbol,
        superMapping: Map<Int, Int>,
        session: FirSession,
    ): Map<Int, Int> {
        val substitutor = createSubstitutionForSupertype(superType, session)
        return buildMap {
            for (entry in superMapping) {
                val subjectIndex = entry.key
                val superIndex = entry.value
                val argument = superType.typeArguments.getOrNull(superIndex) ?: continue
                // Only a plain invariant type parameter reference counts as "directly passed".
                val argumentType = argument as? ConeKotlinType
                val referencedParameter = argumentType?.typeParameterSymbolOrNull() ?: continue
                val klassIndex = klass.typeParameterSymbols.indexOf(referencedParameter)
                if (klassIndex < 0) continue
                val superParameter = superClass.typeParameterSymbols.getOrNull(superIndex) ?: continue
                if (haveSameConstraints(referencedParameter, superParameter, substitutor)) {
                    put(subjectIndex, klassIndex)
                }
            }
        }
    }

    /**
     * A subject parameter is inherited only if it is inherited through every inheritance path,
     * ending up in the same parameter position.
     */
    private fun intersectMappings(first: Map<Int, Int>, second: Map<Int, Int>): Map<Int, Int> {
        return first.filter { entry -> second[entry.key] == entry.value }
    }

    private fun haveSameConstraints(
        subParameter: FirTypeParameterSymbol,
        superParameter: FirTypeParameterSymbol,
        superSubstitutor: ConeSubstitutor,
    ): Boolean {
        if (subParameter.variance != superParameter.variance) return false
        val subBounds = subParameter.nonTrivialBounds(ConeSubstitutor.Empty)
        val superBounds = superParameter.nonTrivialBounds(superSubstitutor)
        return subBounds == superBounds
    }

    /**
     * A type argument is generalized if it is a star projection or a type parameter
     * with no more restrictive constraints than the corresponding parameter of the subject's class.
     */
    private fun isGeneralized(
        argument: ConeTypeProjection,
        subjectParameter: FirTypeParameterSymbol,
        subjectSubstitutor: ConeSubstitutor,
    ): Boolean {
        if (argument is ConeStarProjection) return true
        val argumentType = argument.type ?: return true
        val argumentParameter = argumentType.typeParameterSymbolOrNull() ?: return false
        val argumentBounds = argumentParameter.nonTrivialBounds(ConeSubstitutor.Empty)
        val expectedBounds = subjectParameter.nonTrivialBounds(subjectSubstitutor)
        return expectedBounds.containsAll(argumentBounds)
    }

    private fun ConeKotlinType.typeParameterSymbolOrNull(): FirTypeParameterSymbol? {
        return ((this as? ConeTypeParameterType)?.lookupTag as? ConeTypeParameterLookupTagImpl)?.typeParameterSymbol
    }

    private fun FirTypeParameterSymbol.nonTrivialBounds(substitutor: ConeSubstitutor): Set<ConeKotlinType> {
        return resolvedBounds
            .map { substitutor.substituteOrSelf(it.coneType) }
            .filterNot { it.isNullableAny }
            .toSet()
    }

    /**
     * Substitutes the subject class parameters with the actual subject type arguments,
     * so that parameter bounds can be compared in terms of the use-site type.
     */
    private fun ConeClassLikeType.substitutorForOwnParameters(klass: FirRegularClassSymbol, session: FirSession): ConeSubstitutor {
        val mapping = klass.typeParameterSymbols
            .zip(typeArguments.mapNotNull { it.type })
            .toMap()
        return substitutorByMap(mapping, session)
    }
}
