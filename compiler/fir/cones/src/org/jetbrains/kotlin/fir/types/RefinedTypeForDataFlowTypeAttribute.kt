/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import kotlin.reflect.KClass

/**
 * Needed for the case when the type used for smart casts might differ from the actual expression type.
 *
 * It might happen because we add equality constraint with the expected type to preserve K1 semantics.
 * See [org.jetbrains.kotlin.fir.resolve.inference.FirCallCompleter.isSyntheticFunctionCallThatShouldUseEqualityConstraint]
 *
 * Hopefully, we'll be able to remove it once KT-81083 is fixed.
 */
class RefinedTypeForDataFlowTypeAttribute(
    override val coneType: ConeKotlinType,
) : ConeAttributeWithConeType<RefinedTypeForDataFlowTypeAttribute>() {
    override fun union(other: RefinedTypeForDataFlowTypeAttribute?): RefinedTypeForDataFlowTypeAttribute? = null
    override fun intersect(other: RefinedTypeForDataFlowTypeAttribute?): RefinedTypeForDataFlowTypeAttribute? = null
    override fun add(other: RefinedTypeForDataFlowTypeAttribute?): RefinedTypeForDataFlowTypeAttribute = other ?: this
    override fun isSubtypeOf(other: RefinedTypeForDataFlowTypeAttribute?): Boolean = true
    override fun toString(): String = "{${coneType.renderForDebugging()}=}"
    override fun copyWith(newType: ConeKotlinType): RefinedTypeForDataFlowTypeAttribute = RefinedTypeForDataFlowTypeAttribute(newType)

    override val key: KClass<out RefinedTypeForDataFlowTypeAttribute>
        get() = RefinedTypeForDataFlowTypeAttribute::class
    override val keepInInferredDeclarationType: Boolean
        get() = true
}

val ConeAttributes.refinedTypeForDataFlow: RefinedTypeForDataFlowTypeAttribute? by ConeAttributes.attributeAccessor<RefinedTypeForDataFlowTypeAttribute>()

val ConeKotlinType.refinedTypeForDataFlowOrSelf: ConeKotlinType
    get() = attributes.refinedTypeForDataFlow?.coneType ?: this
