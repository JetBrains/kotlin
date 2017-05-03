/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.load.java.typeEnhancement

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.load.java.*
import org.jetbrains.kotlin.load.java.typeEnhancement.MutabilityQualifier.MUTABLE
import org.jetbrains.kotlin.load.java.typeEnhancement.MutabilityQualifier.READ_ONLY
import org.jetbrains.kotlin.load.java.typeEnhancement.NullabilityQualifier.NOT_NULL
import org.jetbrains.kotlin.load.java.typeEnhancement.NullabilityQualifier.NULLABLE
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import java.util.*

enum class NullabilityQualifier {
    NULLABLE,
    NOT_NULL
}

enum class MutabilityQualifier {
    READ_ONLY,
    MUTABLE
}

class JavaTypeQualifiers internal constructor(
        val nullability: NullabilityQualifier?,
        val mutability: MutabilityQualifier?,
        internal val isNotNullTypeParameter: Boolean
) {
    companion object {
        val NONE = JavaTypeQualifiers(null, null, false)
    }
}

private fun KotlinType.extractQualifiers(): JavaTypeQualifiers {
    val (lower, upper) =
            if (this.isFlexible())
                asFlexibleType().let { Pair(it.lowerBound, it.upperBound) }
            else Pair(this, this)

    val mapping = JavaToKotlinClassMap
    return JavaTypeQualifiers(
            if (lower.isMarkedNullable) NULLABLE else if (!upper.isMarkedNullable) NOT_NULL else null,
            if (mapping.isReadOnly(lower)) READ_ONLY else if (mapping.isMutable(upper)) MUTABLE else null,
            isNotNullTypeParameter = unwrap() is NotNullTypeParameter)
}

private fun KotlinType.extractQualifiersFromAnnotations(): JavaTypeQualifiers {
    fun <T: Any> List<FqName>.ifPresent(qualifier: T) = if (any { annotations.findAnnotation(it) != null}) qualifier else null

    // These two overloads are just for sake of optimization as in most cases last parameter in second overload is null
    fun <T: Any> uniqueNotNull(x: T?, y: T?) = if (x == null || y == null || x == y) x ?: y else null
    fun <T: Any> uniqueNotNull(a: T?, b: T?, c: T?) =
            if (c == null)
                uniqueNotNull(a, b)
            else
                listOf(a, b, c).filterNotNull().toSet().singleOrNull()

    // Javax/FundBugs NonNull annotation has parameter `when` that determines actual nullability
    fun FqName.extractQualifierFromAnnotationWithWhen(): NullabilityQualifier? {
        val annotationDescriptor = annotations.findAnnotation(this) ?: return null
        return annotationDescriptor.allValueArguments.values.singleOrNull()?.value?.let {
            enumEntryDescriptor ->
            if (enumEntryDescriptor !is ClassDescriptor) return@let null
            if (enumEntryDescriptor.name.asString() == "ALWAYS") NOT_NULL else NULLABLE
        } ?: NOT_NULL
    }

    val nullability = uniqueNotNull(
            NULLABLE_ANNOTATIONS.ifPresent(NULLABLE),
            NOT_NULL_ANNOTATIONS.ifPresent(NOT_NULL),
            JAVAX_NONNULL_ANNOTATION.extractQualifierFromAnnotationWithWhen()
    )
    return JavaTypeQualifiers(
            nullability,
            uniqueNotNull(READ_ONLY_ANNOTATIONS.ifPresent(READ_ONLY), MUTABLE_ANNOTATIONS.ifPresent(MUTABLE)),
            isNotNullTypeParameter = nullability == NOT_NULL && isTypeParameter()
    )
}

fun KotlinType.computeIndexedQualifiersForOverride(fromSupertypes: Collection<KotlinType>, isCovariant: Boolean): (Int) -> JavaTypeQualifiers {
    fun KotlinType.toIndexed(): List<KotlinType> {
        val list = ArrayList<KotlinType>(1)

        fun add(type: KotlinType) {
            list.add(type)
            for (arg in type.arguments) {
                if (arg.isStarProjection) {
                    list.add(arg.type)
                }
                else {
                    add(arg.type)
                }
            }
        }

        add(this)
        return list
    }

    val indexedFromSupertypes = fromSupertypes.map { it.toIndexed() }
    val indexedThisType = this.toIndexed()

    // The covariant case may be hard, e.g. in the superclass the return may be Super<T>, but in the subclass it may be Derived, which
    // is declared to extend Super<T>, and propagating data here is highly non-trivial, so we only look at the head type constructor
    // (outermost type), unless the type in the subclass is interchangeable with the all the types in superclasses:
    // e.g. we have (Mutable)List<String!>! in the subclass and { List<String!>, (Mutable)List<String>! } from superclasses
    // Note that `this` is flexible here, so it's equal to it's bounds
    val onlyHeadTypeConstructor = isCovariant && fromSupertypes.any { !KotlinTypeChecker.DEFAULT.equalTypes(it, this) }

    val treeSize = if (onlyHeadTypeConstructor) 1 else indexedThisType.size
    val computedResult = Array(treeSize) {
        index ->
        val isHeadTypeConstructor = index == 0
        assert(isHeadTypeConstructor || !onlyHeadTypeConstructor) { "Only head type constructors should be computed" }

        val qualifiers = indexedThisType[index]
        val verticalSlice = indexedFromSupertypes.mapNotNull { it.getOrNull(index) }

        // Only the head type constructor is safely co-variant
        qualifiers.computeQualifiersForOverride(verticalSlice, isCovariant && isHeadTypeConstructor)
    }

    return { index -> computedResult.getOrElse(index) { JavaTypeQualifiers.NONE } }
}

private fun KotlinType.computeQualifiersForOverride(fromSupertypes: Collection<KotlinType>, isCovariant: Boolean): JavaTypeQualifiers {
    val nullabilityFromSupertypes = fromSupertypes.mapNotNull { it.extractQualifiers().nullability }.toSet()
    val mutabilityFromSupertypes = fromSupertypes.mapNotNull { it.extractQualifiers().mutability }.toSet()

    val own = extractQualifiersFromAnnotations()

    val isAnyNonNullTypeParameter = own.isNotNullTypeParameter || fromSupertypes.any { it.extractQualifiers().isNotNullTypeParameter }

    fun createJavaTypeQualifiers(nullability: NullabilityQualifier?, mutability: MutabilityQualifier?): JavaTypeQualifiers {
        if (!isAnyNonNullTypeParameter || nullability != NOT_NULL) return JavaTypeQualifiers(nullability, mutability, false)
        return JavaTypeQualifiers(
                nullability, mutability,
                isNotNullTypeParameter = true)
    }

    if (isCovariant) {
        fun <T : Any> Set<T>.selectCovariantly(low: T, high: T, own: T?): T? {
            val supertypeQualifier = if (low in this) low else if (high in this) high else null
            return if (supertypeQualifier == low && own == high) null else own ?: supertypeQualifier
        }
        return createJavaTypeQualifiers(
                nullabilityFromSupertypes.selectCovariantly(NOT_NULL, NULLABLE, own.nullability),
                mutabilityFromSupertypes.selectCovariantly(MUTABLE, READ_ONLY, own.mutability)
        )
    }
    else {
        fun <T : Any> Set<T>.selectInvariantly(own: T?): T? {
            val effectiveSet = own?.let { (this + own).toSet() } ?: this
            // if this set contains exactly one element, it is the qualifier everybody agrees upon,
            // otherwise (no qualifiers, or multiple qualifiers), there's no single such qualifier
            // and all qualifiers are discarded
            return effectiveSet.singleOrNull()
        }
        return createJavaTypeQualifiers(
                nullabilityFromSupertypes.selectInvariantly(own.nullability),
                mutabilityFromSupertypes.selectInvariantly(own.mutability)
        )
    }
}
