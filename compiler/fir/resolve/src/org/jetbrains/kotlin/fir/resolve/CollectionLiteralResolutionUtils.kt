/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.diagnostics.ConeCollectionLiteralAmbiguity
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeUnsupportedCollectionLiteralType
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionContext
import org.jetbrains.kotlin.fir.resolve.inference.CollectionLiteralBounds
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeCapturedType
import org.jetbrains.kotlin.fir.types.ConeDefinitelyNotNullType
import org.jetbrains.kotlin.fir.types.ConeDynamicType
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.ConeIntegerLiteralType
import org.jetbrains.kotlin.fir.types.ConeIntersectionType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeLookupTagBasedType
import org.jetbrains.kotlin.fir.types.ConeStubType
import org.jetbrains.kotlin.fir.types.ConeTypeVariableType

context(resolutionContext: ResolutionContext)
fun ConeKotlinType.getClassRepresentativeForCollectionLiteralResolution(): FirRegularClassSymbol? {
    return when (this) {
        is ConeFlexibleType -> lowerBound.getClassRepresentativeForCollectionLiteralResolution()
        is ConeCapturedType -> constructor.lowerType?.getClassRepresentativeForCollectionLiteralResolution()
        is ConeDefinitelyNotNullType -> {
            // very rarely, but still needed, because there might be an expected type of form `Captured(in SomeCollection?) & Any`
            original.getClassRepresentativeForCollectionLiteralResolution()
        }
        is ConeDynamicType,
        is ConeIntersectionType,
        is ConeStubType,
        is ConeTypeVariableType,
        is ConeIntegerLiteralType,
            -> null
        is ConeLookupTagBasedType ->
            when (val symbol = lookupTag.toSymbol()) {
                is FirTypeParameterSymbol, is FirAnonymousObjectSymbol, null -> null
                is FirRegularClassSymbol -> symbol
                is FirTypeAliasSymbol -> fullyExpandedType().getClassRepresentativeForCollectionLiteralResolution()
            }
    }
}

/**
 * For Kotlin class:
 *  There is a companion && in this companion at least one operator `of` is declared.
 *  If all the `of` operators are deprecated with `level=HIDDEN`, the class must be considered as not-having `of` operator
 *   ([KT-83165](https://youtrack.jetbrains.com/issue/KT-83165)).
 *  If the overloads of `of` are not visible from the call-site, the class must be considered as not-having `of` operator
 *   ([KT-84072](https://youtrack.jetbrains.com/issue/KT-84072)).
 *
 * For Java class:
 *  Static `of` with vararg is declared which fulfills all the restrictions on `of` (KT-80494).
 *
 * Additionally:
 *  `List`, `Set`, `MutableList`, `MutableSet`, `Sequence`, `Array`, primitive arrays, and unsigned arrays all declare operator `of`.
 *  ([KT-81722](https://youtrack.jetbrains.com/issue/KT-81722) for stdlib support).
 */
context(resolutionContext: ResolutionContext)
fun FirRegularClassSymbol.declaresOperatorOf(): Boolean {
    return tryAllCLResolutionStrategies {
        if (declaresOperatorOf(this@declaresOperatorOf)) true
        else null
    } ?: false
}

context(context: ResolutionContext)
fun Collection<FirRegularClassSymbol>.chooseSingleClassFromIntersectionComponents(): FirRegularClassSymbol? {
    return firstOrNull { candidate ->
        all { other ->
            candidate.fir.isSubclassOf(other.toLookupTag(), context.session, isStrict = false)
        }
    }
}

fun CollectionLiteralBounds?.toConeDiagnostic(): ConeDiagnostic {
    return when (this) {
        is CollectionLiteralBounds.Ambiguity -> ConeCollectionLiteralAmbiguity(bounds.toList())
        else -> ConeUnsupportedCollectionLiteralType
    }
}