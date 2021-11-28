/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.typeEnhancement

import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.model.KotlinTypeMarker

private fun <T : Any> Set<T>.select(low: T, high: T, own: T?, isCovariant: Boolean): T? {
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

private fun Set<NullabilityQualifier>.select(own: NullabilityQualifier?, isCovariant: Boolean) =
    if (own == NullabilityQualifier.FORCE_FLEXIBILITY)
        NullabilityQualifier.FORCE_FLEXIBILITY
    else
        select(NullabilityQualifier.NOT_NULL, NullabilityQualifier.NULLABLE, own, isCovariant)

private val JavaTypeQualifiers.nullabilityForErrors: NullabilityQualifier?
    get() = if (isNullabilityQualifierForWarning) null else nullability

fun JavaTypeQualifiers.computeQualifiersForOverride(
    superQualifiers: Collection<JavaTypeQualifiers>,
    isCovariant: Boolean,
    isForVarargParameter: Boolean,
    ignoreDeclarationNullabilityAnnotations: Boolean
): JavaTypeQualifiers {
    val newNullabilityForErrors = superQualifiers.mapNotNull { it.nullabilityForErrors }.toSet()
        .select(nullabilityForErrors, isCovariant)
    val newNullability = newNullabilityForErrors ?: superQualifiers.mapNotNull { it.nullability }.toSet()
        .select(nullability, isCovariant)
    val newMutability = superQualifiers.mapNotNull { it.mutability }.toSet()
        .select(MutabilityQualifier.MUTABLE, MutabilityQualifier.READ_ONLY, mutability, isCovariant)
    // Vararg value parameters effectively have non-nullable type in Kotlin
    // and having nullable types in Java may lead to impossibility of overriding them in Kotlin
    val realNullability = newNullability?.takeUnless {
        ignoreDeclarationNullabilityAnnotations || (isForVarargParameter && it == NullabilityQualifier.NULLABLE)
    }
    return JavaTypeQualifiers(
        realNullability, newMutability,
        realNullability == NullabilityQualifier.NOT_NULL && (definitelyNotNull || superQualifiers.any { it.definitelyNotNull }),
        realNullability != null && newNullabilityForErrors != newNullability
    )
}

fun TypeSystemCommonBackendContext.hasEnhancedNullability(type: KotlinTypeMarker): Boolean =
    type.hasAnnotation(JvmAnnotationNames.ENHANCED_NULLABILITY_ANNOTATION)
