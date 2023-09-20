/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.enhancement

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.fir.resolve.substitution.AbstractConeSubstitutor
import org.jetbrains.kotlin.fir.types.*
import kotlin.reflect.KClass

class EnhancedTypeForWarningAttribute(
    override val coneType: ConeKotlinType,
) : ConeAttributeWithConeType<EnhancedTypeForWarningAttribute>() {
    override fun union(other: EnhancedTypeForWarningAttribute?): EnhancedTypeForWarningAttribute? = null
    override fun intersect(other: EnhancedTypeForWarningAttribute?): EnhancedTypeForWarningAttribute? = null
    override fun add(other: EnhancedTypeForWarningAttribute?): EnhancedTypeForWarningAttribute = other ?: this
    override fun isSubtypeOf(other: EnhancedTypeForWarningAttribute?): Boolean = true
    override fun toString(): String = "Enhanced for warning(${coneType.renderForDebugging()})"
    override fun copyWith(newType: ConeKotlinType): EnhancedTypeForWarningAttribute = EnhancedTypeForWarningAttribute(newType)

    override val key: KClass<out EnhancedTypeForWarningAttribute>
        get() = EnhancedTypeForWarningAttribute::class

    override val keepInInferredDeclarationType: Boolean
        get() = true
}

val ConeAttributes.enhancedTypeForWarning: EnhancedTypeForWarningAttribute? by ConeAttributes.attributeAccessor<EnhancedTypeForWarningAttribute>()

val ConeKotlinType.enhancedTypeForWarning: ConeKotlinType?
    get() = attributes.enhancedTypeForWarning?.coneType

/**
 * Substitutor that substitutes types with their [ConeKotlinType.enhancedTypeForWarning] recursively.
 */
class EnhancedForWarningConeSubstitutor(typeContext: ConeTypeContext) : AbstractConeSubstitutor(typeContext) {
    override fun substituteType(type: ConeKotlinType): ConeKotlinType? {
        // The attribute is usually taken from the lower bound of flexible types.
        // However, if the type has flexible mutability, we don't want to accidentally enhance the mutability to the lower bound.
        if (type is ConeFlexibleType && type.hasFlexibleMutability()) {
            val lowerSubstituted = substituteOrNull(type.lowerBound)

            return if (lowerSubstituted is ConeSimpleKotlinType) {
                ConeFlexibleType(
                    lowerBound = lowerSubstituted,
                    upperBound = substituteOrNull(type.upperBound) as? ConeSimpleKotlinType ?: type.upperBound
                )
            } else {
                null
            }
        }

        // If the top-level type can be enhanced, this will only enhance the top-level type but not its arguments: Foo<Bar!>! -> Foo<Bar!>?
        // Otherwise, it will enhance recursively until the first possible enhancement.
        val enhancedTopLevel = type.enhancedTypeForWarning

        // This will also enhance type arguments if the top-level type was enhanced, otherwise it will continue enhancing recursively.
        return enhancedTopLevel?.let(::substituteOrSelf)
    }

    private fun ConeFlexibleType.hasFlexibleMutability(): Boolean {
        return JavaToKotlinClassMap.isMutable(lowerBound.classId) && JavaToKotlinClassMap.isReadOnly(upperBound.classId)
    }
}