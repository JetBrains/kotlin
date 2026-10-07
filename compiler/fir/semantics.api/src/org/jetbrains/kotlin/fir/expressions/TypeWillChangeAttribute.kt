/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.types.ConeAttribute
import org.jetbrains.kotlin.fir.types.ConeAttributes
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.withAttributes
import kotlin.reflect.KClass

/**
 * Contains the type that will be present in place of the current one,
 * when the corresponding [LanguageFeature] gets enabled.
 * Present for the duration of the feature's deprecation cycle to provide deprecation warnings.
 */
data class TypeWillChangeAttribute(
    val newType: ConeKotlinType,
    val languageFeature: LanguageFeature,
) : ConeAttribute<TypeWillChangeAttribute>() {
    // Those methods should not matter too much because it's only assumed to be used for explicit type arguments
    // for which we don't expect to perform complex operations
    override fun union(other: TypeWillChangeAttribute?): TypeWillChangeAttribute? = null
    override fun intersect(other: TypeWillChangeAttribute?): TypeWillChangeAttribute? = null
    override fun add(other: TypeWillChangeAttribute?): TypeWillChangeAttribute = other ?: this
    override fun isSubtypeOf(other: TypeWillChangeAttribute?): Boolean = true

    override val key: KClass<out TypeWillChangeAttribute>
        get() = TypeWillChangeAttribute::class

    override val keepInInferredDeclarationType: Boolean
        get() = true
}

val ConeAttributes.typeWillChangeAttribute: TypeWillChangeAttribute? by ConeAttributes.attributeAccessor()

fun ConeKotlinType.typeChangeRelatedTo(feature: LanguageFeature): TypeWillChangeAttribute? =
    attributes.typeWillChangeAttribute?.takeIf { it.languageFeature == feature }

fun <T : ConeKotlinType> T.withNewTypeSince(feature: LanguageFeature, newType: T): T =
    withAttributes(attributes.add(TypeWillChangeAttribute(newType, feature)))
