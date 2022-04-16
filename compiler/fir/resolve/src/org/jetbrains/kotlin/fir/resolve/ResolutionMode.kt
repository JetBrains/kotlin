/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef

sealed class ResolutionMode {
    object ContextDependent : ResolutionMode()
    object ContextDependentDelegate : ResolutionMode()
    object ContextIndependent : ResolutionMode()
    object ReceiverResolution : ResolutionMode()

    // TODO: it's better not to use WithExpectedType(FirImplicitTypeRef)
    class WithExpectedType(
        val expectedTypeRef: FirTypeRef,
        val mayBeCoercionToUnitApplied: Boolean = false,
        val expectedTypeMismatchIsReportedInChecker: Boolean = false,
    ) : ResolutionMode()

    class WithStatus(val status: FirDeclarationStatus) : ResolutionMode()

    class LambdaResolution(val expectedReturnTypeRef: FirResolvedTypeRef?) : ResolutionMode()

    class WithExpectedTypeFromCast(
        val expectedTypeRef: FirTypeRef,
    ) : ResolutionMode()

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
    ) : ResolutionMode()
}

fun ResolutionMode.expectedType(components: BodyResolveComponents, allowFromCast: Boolean = false): FirTypeRef? = when (this) {
    is ResolutionMode.WithExpectedType -> expectedTypeRef
    is ResolutionMode.ContextIndependent,
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
