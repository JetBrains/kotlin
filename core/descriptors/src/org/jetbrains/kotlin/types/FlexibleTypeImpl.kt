/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererOptions
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.util.isFlexible

class FlexibleTypeImpl(lowerBound: SimpleType, upperBound: SimpleType) : FlexibleType(lowerBound, upperBound), CustomTypeParameter {
    companion object {
        @JvmField
        var RUN_SLOW_ASSERTIONS = false
    }

    // These assertions are needed for checking invariants of flexible types.
    //
    // Unfortunately isSubtypeOf is running resolve for lazy types.
    // Because of this we can't run these assertions when we are creating this type. See EA-74904
    //
    // Also isSubtypeOf is not a very fast operation, so we are running assertions only if ASSERTIONS_ENABLED. See KT-7540
    private var assertionsDone = false

    private fun runAssertions() {
        if (!RUN_SLOW_ASSERTIONS || assertionsDone) return
        assertionsDone = true

        assert(!lowerBound.isFlexible()) { "Lower bound of a flexible type can not be flexible: $lowerBound" }
        assert(!upperBound.isFlexible()) { "Upper bound of a flexible type can not be flexible: $upperBound" }
        assert(lowerBound != upperBound) { "Lower and upper bounds are equal: $lowerBound == $upperBound" }
        assert(KotlinTypeChecker.DEFAULT.isSubtypeOf(lowerBound, upperBound)) {
            "Lower bound $lowerBound of a flexible type must be a subtype of the upper bound $upperBound"
        }
    }

    override val delegate: SimpleType
        get() {
            runAssertions()
            return lowerBound
        }

    override val isTypeParameter: Boolean
        get() = lowerBound.constructor.declarationDescriptor is TypeParameterDescriptor
                && lowerBound.constructor == upperBound.constructor

    override fun substitutionResult(replacement: KotlinType): KotlinType {
        val unwrapped = replacement.unwrap()
        return when (unwrapped) {
            is FlexibleType -> unwrapped
            is SimpleType -> KotlinTypeFactory.flexibleType(unwrapped, unwrapped.makeNullableAsSpecified(true))
        }.inheritEnhancement(unwrapped)
    }

    override fun replaceAttributes(newAttributes: TypeAttributes): UnwrappedType =
        KotlinTypeFactory.flexibleType(lowerBound.replaceAttributes(newAttributes), upperBound.replaceAttributes(newAttributes))

    override fun render(renderer: DescriptorRenderer, options: DescriptorRendererOptions): String {
        if (options.debugMode) {
            return "(${renderer.renderType(lowerBound)}..${renderer.renderType(upperBound)})"
        }
        return renderer.renderFlexibleType(renderer.renderType(lowerBound), renderer.renderType(upperBound), builtIns)
    }

    override fun toString() = "($lowerBound..$upperBound)"

    override fun makeNullableAsSpecified(newNullability: Boolean): UnwrappedType = KotlinTypeFactory.flexibleType(
        lowerBound.makeNullableAsSpecified(newNullability),
        upperBound.makeNullableAsSpecified(newNullability)
    )

    @TypeRefinement
    @OptIn(TypeRefinement::class)
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner): FlexibleType {
        return FlexibleTypeImpl(
            kotlinTypeRefiner.refineType(lowerBound) as SimpleType,
            kotlinTypeRefiner.refineType(upperBound) as SimpleType
        )
    }
}
