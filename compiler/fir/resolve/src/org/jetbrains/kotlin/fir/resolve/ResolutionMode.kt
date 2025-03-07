/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.ResolutionMode.ArrayLiteralPosition
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

    @OptIn(WithExpectedType.ExpectedTypeRefAccess::class)
    class WithExpectedType(
        @property:ExpectedTypeRefAccess
        val expectedTypeRef: FirResolvedTypeRef,
        val mayBeCoercionToUnitApplied: Boolean = false,
        val expectedTypeMismatchIsReportedInChecker: Boolean = false,
        val fromCast: Boolean = false,
        /**
         * Expected type is used for inferring array literal types in places where array literal syntax is supported
         * Currently, it's an argument of annotation call or a default value of parameter in annotation class constructor
         * `ArrayLiteralPosition.AnnotationArgument` does not produce a constraint during completion because
         * it can contain type parameter types which aren't substituted to type variable types.
         */
        val arrayLiteralPosition: ArrayLiteralPosition? = null,
        /**
         * It might be ok if the types turn out to be incompatible.
         * Consider the following examples with properties and their backing fields:
         *
         * ```
         * val items: List field = mutableListOf()
         * val s: String field = 10 get() = ...
         * ```
         *
         * In these examples we should try using the property type information while resolving the initializer,
         * but it's ok if it's not applicable
         */
        val shouldBeStrictlyEnforced: Boolean = true,
        /** Currently the only case for expected type when we don't force completion are when's branches */
        forceFullCompletion: Boolean = true,
    ) : ResolutionMode(forceFullCompletion) {

        @RequiresOptIn(
            "Accessing 'expectedTypeRef' is generally not necessary unless the caller needs access to its source. " +
                    "Prefer using 'expectedType' instead."
        )
        annotation class ExpectedTypeRefAccess

        val expectedType: ConeKotlinType get() = expectedTypeRef.coneType

        fun copy(
            expectedTypeRef: FirResolvedTypeRef = this.expectedTypeRef,
            mayBeCoercionToUnitApplied: Boolean = this.mayBeCoercionToUnitApplied,
            forceFullCompletion: Boolean = this.forceFullCompletion,
            shouldBeStrictlyEnforced: Boolean = this.shouldBeStrictlyEnforced,
        ): WithExpectedType = WithExpectedType(
            expectedTypeRef = expectedTypeRef,
            mayBeCoercionToUnitApplied = mayBeCoercionToUnitApplied,
            expectedTypeMismatchIsReportedInChecker = expectedTypeMismatchIsReportedInChecker,
            fromCast = fromCast,
            arrayLiteralPosition = arrayLiteralPosition,
            shouldBeStrictlyEnforced = shouldBeStrictlyEnforced,
            forceFullCompletion = forceFullCompletion
        )

        override fun toString(): String {
            return "WithExpectedType: ${expectedTypeRef.prettyString()}, " +
                    "mayBeCoercionToUnitApplied=${mayBeCoercionToUnitApplied}, " +
                    "expectedTypeMismatchIsReportedInChecker=${expectedTypeMismatchIsReportedInChecker}, " +
                    "fromCast=${fromCast}, " +
                    "arrayLiteralPosition=${arrayLiteralPosition}, " +
                    "shouldBeStrictlyEnforced=${shouldBeStrictlyEnforced}, " +
                    "forceFullCompletion=${forceFullCompletion}, "
        }
    }

    enum class ArrayLiteralPosition {
        AnnotationArgument,
        AnnotationParameter,
    }

    class WithStatus(val status: FirDeclarationStatus) : ResolutionMode(forceFullCompletion = false) {
        override fun toString(): String {
            return "WithStatus: ${status.render()}"
        }
    }

    /**
     * Should be used only for type refs transformations, it forces to replace implicit type refs.
     * For other cases, it works just like [ContextIndependent], i.e., resolves yet unresolved explicit type references.
     *
     * See [org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformerDispatcher.transformImplicitTypeRef]
     */
    class UpdateImplicitTypeRef(val newTypeRef: FirResolvedTypeRef) : ResolutionMode(forceFullCompletion = false)

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

val ResolutionMode.expectedType: ConeKotlinType?
    get() = when (this) {
        is ResolutionMode.WithExpectedType -> expectedType.takeIf { !this.fromCast }
        else -> null
    }

fun withExpectedType(
    expectedTypeRef: FirTypeRef,
    expectedTypeMismatchIsReportedInChecker: Boolean = false,
    arrayLiteralPosition: ArrayLiteralPosition? = null,
): ResolutionMode = when {
    expectedTypeRef is FirResolvedTypeRef -> ResolutionMode.WithExpectedType(
        expectedTypeRef,
        expectedTypeMismatchIsReportedInChecker = expectedTypeMismatchIsReportedInChecker,
        arrayLiteralPosition = arrayLiteralPosition,
    )
    else -> ResolutionMode.ContextIndependent
}

@JvmName("withExpectedTypeNullable")
fun withExpectedType(coneType: ConeKotlinType?, mayBeCoercionToUnitApplied: Boolean = false): ResolutionMode {
    return coneType?.let { withExpectedType(it, mayBeCoercionToUnitApplied) } ?: ResolutionMode.ContextDependent
}

fun withExpectedType(coneType: ConeKotlinType, mayBeCoercionToUnitApplied: Boolean = false): ResolutionMode {
    val typeRef = buildResolvedTypeRef {
        this.coneType = coneType
    }
    return ResolutionMode.WithExpectedType(typeRef, mayBeCoercionToUnitApplied)
}

fun FirDeclarationStatus.mode(): ResolutionMode =
    ResolutionMode.WithStatus(this)
