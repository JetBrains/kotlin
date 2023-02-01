/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef

sealed class ResolutionMode {
    object ContextDependent : ResolutionMode() {
        override fun toString(): String = "ContextDependent"
    }

    object ContextDependentDelegate : ResolutionMode() {
        override fun toString(): String = "ContextDependentDelegate"
    }

    object ContextIndependent : ResolutionMode() {
        override fun toString(): String = "ContextIndependent"
    }

    object ReceiverResolution : ResolutionMode() {
        override fun toString(): String = "ReceiverResolution"
    }

    // TODO: it's better not to use WithExpectedType(FirImplicitTypeRef)
    class WithExpectedType(
        val expectedTypeRef: FirTypeRef,
        val mayBeCoercionToUnitApplied: Boolean = false,
        val expectedTypeMismatchIsReportedInChecker: Boolean = false,
    ) : ResolutionMode() {
        override fun toString(): String {
            return "WithExpectedType: ${expectedTypeRef.prettyString()}"
        }
    }

    class WithStatus(val status: FirDeclarationStatus) : ResolutionMode() {
        override fun toString(): String {
            return "WithStatus: ${status.render()}"
        }
    }

    class LambdaResolution(val expectedReturnTypeRef: FirResolvedTypeRef?) : ResolutionMode() {
        override fun toString(): String {
            return "LambdaResolution: ${expectedReturnTypeRef.prettyString()}"
        }
    }

    class WithExpectedTypeFromCast(
        val expectedTypeRef: FirTypeRef,
    ) : ResolutionMode() {
        override fun toString(): String {
            return "WithExpectedTypeFromCast: ${expectedTypeRef.prettyString()}"
        }
    }

    /**
     * This resolution mode is similar to
     * WithExpectedType, but it's ok if the
     * types turn out to be incompatible.
     * Consider the following examples with
     * properties and their backing fields:
     *
     * val items: List<T>
     *     field = mutableListOf()
     *
     * val s: String
     *     field = 10
     *     get() = ...
     *
     * In these examples we should try using
     * the property type information while
     * resolving the initializer, but it's ok
     * if it's not applicable.
     */
    class WithSuggestedType(
        val suggestedTypeRef: FirTypeRef,
    ) : ResolutionMode() {
        override fun toString(): String {
            return "WithSuggestedType: ${suggestedTypeRef.prettyString()}"
        }
    }

    /**
     * This resolution mode is used for resolving the LHS of assignments.
     *
     * It is generally treated like [ContextIndependent], however it carries the containing assignment which is needed for
     * call-site-dependant resolution to detect cases where the setter of the called property needs to be considered instead of the getter.
     *
     * Examples are:
     *
     * - assigning to a property with a setter that has a different visibility than the property
     * - assigning to a non-deprecated property with a setter that is deprecated
     */
    class AssignmentLValue(val variableAssignment: FirVariableAssignment) : ResolutionMode() {
        override fun toString(): String = "AssignmentLValue: ${variableAssignment.render()}"
    }

    private companion object {
        private fun FirTypeRef?.prettyString(): String {
            if (this == null) return "null"
            val coneType = this.coneTypeSafe<ConeKotlinType>() ?: return this.render()
            return coneType.renderForDebugging()
        }
    }
}

fun ResolutionMode.expectedType(components: BodyResolveComponents, allowFromCast: Boolean = false): FirTypeRef? = when (this) {
    is ResolutionMode.WithExpectedType -> expectedTypeRef
    is ResolutionMode.ContextIndependent,
    is ResolutionMode.AssignmentLValue,
    is ResolutionMode.ReceiverResolution -> components.noExpectedType
    is ResolutionMode.WithExpectedTypeFromCast -> expectedTypeRef.takeIf { allowFromCast }
    is ResolutionMode.WithSuggestedType -> suggestedTypeRef
    else -> null
}

fun withExpectedType(expectedTypeRef: FirTypeRef?, expectedTypeMismatchIsReportedInChecker: Boolean = false): ResolutionMode =
    expectedTypeRef?.let {
        ResolutionMode.WithExpectedType(
            it,
            expectedTypeMismatchIsReportedInChecker = expectedTypeMismatchIsReportedInChecker
        )
    } ?: ResolutionMode.ContextDependent

@JvmName("withExpectedTypeNullable")
fun withExpectedType(coneType: ConeKotlinType?, mayBeCoercionToUnitApplied: Boolean = false): ResolutionMode {
    return coneType?.let { withExpectedType(it, mayBeCoercionToUnitApplied) } ?: ResolutionMode.ContextDependent
}

fun withExpectedType(coneType: ConeKotlinType, mayBeCoercionToUnitApplied: Boolean = false): ResolutionMode {
    val typeRef = buildResolvedTypeRef {
        type = coneType
    }
    return ResolutionMode.WithExpectedType(typeRef, mayBeCoercionToUnitApplied)
}

fun FirDeclarationStatus.mode(): ResolutionMode =
    ResolutionMode.WithStatus(this)
