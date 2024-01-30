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

sealed class ResolutionMode(
    val forceFullCompletion: Boolean,
) {
    data object ContextDependent : ResolutionMode(forceFullCompletion = false)
    data object Delegate : ResolutionMode(forceFullCompletion = false)
    data object ContextIndependent : ResolutionMode(forceFullCompletion = true)

    sealed class ReceiverResolution(val forCallableReference: Boolean) : ResolutionMode(forceFullCompletion = true) {
        data object ForCallableReference : ReceiverResolution(forCallableReference = true)
        companion object : ReceiverResolution(forCallableReference = false)
    }

    class WithExpectedType(
        val expectedTypeRef: FirResolvedTypeRef,
        val mayBeCoercionToUnitApplied: Boolean = false,
        val expectedTypeMismatchIsReportedInChecker: Boolean = false,
        val fromCast: Boolean = false,
        // It might be ok if the types turn out to be incompatible
        // Consider the following examples with properties and their backing fields:
        //
        // val items: List field = mutableListOf()
        // val s: String field = 10 get() = ...
        // In these examples we should try using the property type information while resolving the initializer,
        // but it's ok if it's not applicable
        val shouldBeStrictlyEnforced: Boolean = true,
        // Currently the only case for expected type when we don't force completion are when's branches
        forceFullCompletion: Boolean = true,
    ) : ResolutionMode(forceFullCompletion) {

        fun copy(
            mayBeCoercionToUnitApplied: Boolean = this.mayBeCoercionToUnitApplied,
            forceFullCompletion: Boolean = this.forceFullCompletion
        ): WithExpectedType = WithExpectedType(
            expectedTypeRef, mayBeCoercionToUnitApplied, expectedTypeMismatchIsReportedInChecker, fromCast, shouldBeStrictlyEnforced,
            forceFullCompletion
        )

        override fun toString(): String {
            return "WithExpectedType: ${expectedTypeRef.prettyString()}, " +
                    "mayBeCoercionToUnitApplied=${mayBeCoercionToUnitApplied}, " +
                    "expectedTypeMismatchIsReportedInChecker=${expectedTypeMismatchIsReportedInChecker}, " +
                    "fromCast=${fromCast}, " +
                    "shouldBeStrictlyEnforced=${shouldBeStrictlyEnforced}, "
        }
    }

    class WithStatus(val status: FirDeclarationStatus) : ResolutionMode(forceFullCompletion = false) {
        override fun toString(): String {
            return "WithStatus: ${status.render()}"
        }
    }

    class LambdaResolution(val expectedReturnTypeRef: FirResolvedTypeRef?) : ResolutionMode(forceFullCompletion = false) {
        override fun toString(): String {
            return "LambdaResolution: ${expectedReturnTypeRef.prettyString()}"
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
    class AssignmentLValue(val variableAssignment: FirVariableAssignment) : ResolutionMode(forceFullCompletion = true) {
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

fun ResolutionMode.expectedType(components: BodyResolveComponents): FirTypeRef? = when (this) {
    is ResolutionMode.WithExpectedType -> expectedTypeRef.takeIf { !this.fromCast }
    is ResolutionMode.ContextIndependent,
    is ResolutionMode.AssignmentLValue,
    is ResolutionMode.ReceiverResolution -> components.noExpectedType
    else -> null
}

fun withExpectedType(expectedTypeRef: FirTypeRef, expectedTypeMismatchIsReportedInChecker: Boolean = false): ResolutionMode = when {
    expectedTypeRef is FirResolvedTypeRef -> ResolutionMode.WithExpectedType(
        expectedTypeRef,
        expectedTypeMismatchIsReportedInChecker = expectedTypeMismatchIsReportedInChecker
    )
    else -> ResolutionMode.ContextIndependent
}

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
