/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.typeEnhancement

fun createJavaTypeQualifiers(
    nullability: NullabilityQualifier?,
    mutability: MutabilityQualifier?,
    forWarning: Boolean,
    isAnyNonNullTypeParameter: Boolean
): JavaTypeQualifiers {
    if (!isAnyNonNullTypeParameter || nullability != NullabilityQualifier.NOT_NULL) {
        return JavaTypeQualifiers(nullability, mutability, false, forWarning)
    }
    return JavaTypeQualifiers(nullability, mutability, true, forWarning)
}

fun <T : Any> Set<T>.select(low: T, high: T, own: T?, isCovariant: Boolean): T? {
    if (isCovariant) {
        val supertypeQualifier = if (low in this) low else if (high in this) high else null
        return if (supertypeQualifier == low && own == high) null else own ?: supertypeQualifier
    }

    // isInvariant
    val effectiveSet = own?.let { (this + own).toSet() } ?: this
    // if this set contains exactly one element, it is the qualifier everybody agrees upon,
    // otherwise (no qualifiers, or multiple qualifiers), there's no single such qualifier
    // and all qualifiers are discarded
    return effectiveSet.singleOrNull()
}

fun Set<NullabilityQualifier>.select(own: NullabilityQualifier?, isCovariant: Boolean) =
    if (own == NullabilityQualifier.FORCE_FLEXIBILITY)
        NullabilityQualifier.FORCE_FLEXIBILITY
    else
        select(NullabilityQualifier.NOT_NULL, NullabilityQualifier.NULLABLE, own, isCovariant)
