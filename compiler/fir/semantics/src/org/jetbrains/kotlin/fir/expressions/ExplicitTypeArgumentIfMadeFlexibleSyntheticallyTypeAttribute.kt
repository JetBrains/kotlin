/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.types.ConeAttribute
import org.jetbrains.kotlin.fir.types.ConeAttributes
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import kotlin.reflect.KClass

/**
 * This attribute is expected to decode original non-flexible type arguments if they were lost
 * after [org.jetbrains.kotlin.fir.resolve.calls.CreateFreshTypeVariableSubstitutorStage.getTypePreservingFlexibilityWrtTypeVariable]
 * TODO: Get rid of this class once KT-59138 is fixed and the relevant feature for disabling it will be removed
 */
data class ExplicitTypeArgumentIfMadeFlexibleSyntheticallyTypeAttribute(
    val coneType: ConeKotlinType,
) : ConeAttribute<ExplicitTypeArgumentIfMadeFlexibleSyntheticallyTypeAttribute>() {
    // Those methods should not matter too much because it's only assumed to be used for explicit type arguments
    // for which we don't expect to perform complex operations
    override fun union(other: ExplicitTypeArgumentIfMadeFlexibleSyntheticallyTypeAttribute?): ExplicitTypeArgumentIfMadeFlexibleSyntheticallyTypeAttribute? =
        null

    override fun intersect(other: ExplicitTypeArgumentIfMadeFlexibleSyntheticallyTypeAttribute?): ExplicitTypeArgumentIfMadeFlexibleSyntheticallyTypeAttribute? =
        null

    override fun add(other: ExplicitTypeArgumentIfMadeFlexibleSyntheticallyTypeAttribute?): ExplicitTypeArgumentIfMadeFlexibleSyntheticallyTypeAttribute =
        other ?: this

    override fun isSubtypeOf(other: ExplicitTypeArgumentIfMadeFlexibleSyntheticallyTypeAttribute?): Boolean = true

    override val key: KClass<out ExplicitTypeArgumentIfMadeFlexibleSyntheticallyTypeAttribute>
        get() = ExplicitTypeArgumentIfMadeFlexibleSyntheticallyTypeAttribute::class

    override val keepInInferredDeclarationType: Boolean
        get() = true

    override val implementsEquality: Boolean
        get() = true
}

val ConeAttributes.explicitTypeArgumentIfMadeFlexibleSynthetically: ExplicitTypeArgumentIfMadeFlexibleSyntheticallyTypeAttribute? by ConeAttributes.attributeAccessor()
