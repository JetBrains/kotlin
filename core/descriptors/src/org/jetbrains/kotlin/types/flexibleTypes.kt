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

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererOptions
import org.jetbrains.kotlin.types.checker.ErrorTypesAreEqualToAnything
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.builtIns


fun KotlinType.isFlexible(): Boolean = unwrap() is FlexibleType
fun KotlinType.asFlexibleType(): FlexibleType = unwrap() as FlexibleType

fun KotlinType.isNullabilityFlexible(): Boolean {
    val flexibility = unwrap() as? FlexibleType ?: return false
    return TypeUtils.isNullableType(flexibility.lowerBound) != TypeUtils.isNullableType(flexibility.upperBound)
}

// This function is intended primarily for sets: since KotlinType.equals() represents _syntactical_ equality of types,
// whereas KotlinTypeChecker.DEFAULT.equalsTypes() represents semantic equality
// A set of types (e.g. exact bounds etc) may contain, for example, X, X? and X!
// These are not equal syntactically (by KotlinType.equals()), but X! is _compatible_ with others as exact bounds,
// moreover, X! is a better fit.
//
// So, we are looking for a type among this set such that it is equal to all others semantically
// (by KotlinTypeChecker.DEFAULT.equalsTypes()), and fits at least as well as they do.
fun Collection<KotlinType>.singleBestRepresentative(): KotlinType? {
    if (this.size == 1) return this.first()

    return this.firstOrNull {
        candidate ->
        this.all {
            other ->
            // We consider error types equal to anything here, so that intersections like
            // {Array<String>, Array<[ERROR]>} work correctly
            candidate == other || ErrorTypesAreEqualToAnything.equalTypes(candidate, other)
        }
    }
}

fun Collection<TypeProjection>.singleBestRepresentative(): TypeProjection? {
    if (this.size == 1) return this.first()

    val projectionKinds = this.map { it.projectionKind }.toSet()
    if (projectionKinds.size != 1) return null

    val bestType = this.map { it.type }.singleBestRepresentative() ?: return null

    return TypeProjectionImpl(projectionKinds.single(), bestType)
}

fun KotlinType.lowerIfFlexible(): SimpleType = with(unwrap()) {
    when (this) {
        is FlexibleType -> lowerBound
        is SimpleType -> this
    }
}
fun KotlinType.upperIfFlexible(): SimpleType = with(unwrap()) {
    when (this) {
        is FlexibleType -> upperBound
        is SimpleType -> this
    }
}

class FlexibleTypeImpl(lowerBound: SimpleType, upperBound: SimpleType) : FlexibleType(lowerBound, upperBound), CustomTypeVariable {
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

        assert (!lowerBound.isFlexible()) { "Lower bound of a flexible type can not be flexible: $lowerBound" }
        assert (!upperBound.isFlexible()) { "Upper bound of a flexible type can not be flexible: $upperBound" }
        assert (lowerBound != upperBound) { "Lower and upper bounds are equal: $lowerBound == $upperBound" }
        assert (KotlinTypeChecker.DEFAULT.isSubtypeOf(lowerBound, upperBound)) {
            "Lower bound $lowerBound of a flexible type must be a subtype of the upper bound $upperBound"
        }
    }

    override val delegate: SimpleType
        get() {
            runAssertions()
            return lowerBound
        }

    override val isTypeVariable: Boolean get() = lowerBound.constructor.declarationDescriptor is TypeParameterDescriptor
                                                 && lowerBound.constructor == upperBound.constructor

    override fun substitutionResult(replacement: KotlinType): KotlinType {
        val unwrapped = replacement.unwrap()
        return when(unwrapped) {
            is FlexibleType -> unwrapped
            is SimpleType -> KotlinTypeFactory.flexibleType(unwrapped, unwrapped.makeNullableAsSpecified(true))
        }.inheritEnhancement(unwrapped)
    }

    override fun replaceAnnotations(newAnnotations: Annotations): UnwrappedType
            = KotlinTypeFactory.flexibleType(lowerBound.replaceAnnotations(newAnnotations), upperBound.replaceAnnotations(newAnnotations))

    override fun render(renderer: DescriptorRenderer, options: DescriptorRendererOptions): String {
        if (options.debugMode) {
            return "(${renderer.renderType(lowerBound)}..${renderer.renderType(upperBound)})"
        }
        return renderer.renderFlexibleType(renderer.renderType(lowerBound), renderer.renderType(upperBound), builtIns)
    }

    override fun makeNullableAsSpecified(newNullability: Boolean): UnwrappedType
            = KotlinTypeFactory.flexibleType(lowerBound.makeNullableAsSpecified(newNullability), upperBound.makeNullableAsSpecified(newNullability))

    override fun refine(moduleDescriptor: ModuleDescriptor) =
        KotlinTypeFactory.flexibleType(lowerBound.refine(moduleDescriptor), upperBound.refine(moduleDescriptor)) as FlexibleType
}
