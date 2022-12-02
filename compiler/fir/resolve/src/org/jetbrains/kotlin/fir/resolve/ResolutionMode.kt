/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef

sealed class ResolutionMode(val forceFullCompletion: Boolean) {
    object ContextDependent : ResolutionMode(forceFullCompletion = false) {
        override fun toString(): String = "ContextDependent"
    }

    object ContextDependentDelegate : ResolutionMode(forceFullCompletion = false) {
        override fun toString(): String = "ContextDependentDelegate"
    }

    object ContextIndependent : ResolutionMode(forceFullCompletion = true) {
        override fun toString(): String = "ContextIndependent"
    }

    object ReceiverResolution : ResolutionMode(forceFullCompletion = true) {
        override fun toString(): String = "ReceiverResolution"
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

    private companion object {
        private fun FirTypeRef?.prettyString(): String {
            if (this == null) return "null"
            val coneType = this.coneTypeSafe<ConeKotlinType>() ?: return this.render()
            return coneType.renderForDebugging()
        }
    }
}

fun ResolutionMode.expectedType(components: BodyResolveComponents, allowFromCast: Boolean = false): FirTypeRef? = when (this) {
    is ResolutionMode.WithExpectedType -> expectedTypeRef.takeIf { !this.fromCast || allowFromCast }
    is ResolutionMode.ContextIndependent,
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
