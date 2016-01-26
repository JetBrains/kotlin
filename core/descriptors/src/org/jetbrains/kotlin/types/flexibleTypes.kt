/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker

interface FlexibleTypeCapabilities {
    fun <T: TypeCapability> getCapability(capabilityClass: Class<T>, jetType: KotlinType, flexibility: Flexibility): T?
    val id: String

    object NONE : FlexibleTypeCapabilities {
        override fun <T : TypeCapability> getCapability(capabilityClass: Class<T>, jetType: KotlinType, flexibility: Flexibility): T? = null
        override val id: String get() = "NONE"
    }
}

interface Flexibility : TypeCapability, SubtypingRepresentatives {
    companion object {
        // This is a "magic" classifier: when type resolver sees it in the code, e.g. ft<Foo, Foo?>, instead of creating a normal type,
        // it creates a flexible type, e.g. (Foo..Foo?).
        // This is used in tests and Evaluate Expression to have flexible types in the code,
        // but normal users should not be referencing this classifier
        val FLEXIBLE_TYPE_CLASSIFIER: ClassId = ClassId.topLevel(FqName("kotlin.internal.flexible.ft"))
    }

    // lowerBound is a subtype of upperBound
    val lowerBound: KotlinType
    val upperBound: KotlinType

    val extraCapabilities: FlexibleTypeCapabilities

    override val subTypeRepresentative: KotlinType
        get() = lowerBound

    override val superTypeRepresentative: KotlinType
        get() = upperBound

    override fun sameTypeConstructor(type: KotlinType) = false
}

fun KotlinType.isFlexible(): Boolean = this.getCapability(Flexibility::class.java) != null
fun KotlinType.flexibility(): Flexibility = this.getCapability(Flexibility::class.java)!!

fun KotlinType.isNullabilityFlexible(): Boolean {
    val flexibility = this.getCapability(Flexibility::class.java) ?: return false
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
            candidate == other || KotlinTypeChecker.ERROR_TYPES_ARE_EQUAL_TO_ANYTHING.equalTypes(candidate, other)
        }
    }
}

fun Collection<TypeProjection>.singleBestRepresentative(): TypeProjection? {
    if (this.size == 1) return this.first()

    val projectionKinds = this.map { it.projectionKind }.toSet()
    if (projectionKinds.size != 1) return null

    val bestType = this.map { it.type }.singleBestRepresentative()
    if (bestType == null) return null

    return TypeProjectionImpl(projectionKinds.single(), bestType)
}

fun KotlinType.lowerIfFlexible(): KotlinType = if (this.isFlexible()) this.flexibility().lowerBound else this
fun KotlinType.upperIfFlexible(): KotlinType = if (this.isFlexible()) this.flexibility().upperBound else this

interface NullAwareness : TypeCapability {
    fun makeNullableAsSpecified(nullable: Boolean): KotlinType
    fun computeIsNullable(): Boolean
}

interface FlexibleTypeDelegation : TypeCapability {
    val delegateType: KotlinType
}

open class DelegatingFlexibleType protected constructor(
        override val lowerBound: KotlinType,
        override val upperBound: KotlinType,
        override val extraCapabilities: FlexibleTypeCapabilities
) : DelegatingType(), NullAwareness, Flexibility, FlexibleTypeDelegation {
    companion object {
        internal val capabilityClasses = hashSetOf(
                NullAwareness::class.java,
                Flexibility::class.java,
                SubtypingRepresentatives::class.java,
                FlexibleTypeDelegation::class.java
        )

        @JvmStatic
        fun create(lowerBound: KotlinType, upperBound: KotlinType, extraCapabilities: FlexibleTypeCapabilities): KotlinType {
            if (lowerBound == upperBound) return lowerBound
            return DelegatingFlexibleType(lowerBound, upperBound, extraCapabilities)
        }

        internal val ASSERTIONS_ENABLED = DelegatingFlexibleType::class.java.desiredAssertionStatus()
    }

     // These assertions are needed for checking invariants of flexible types.
     //
     // Unfortunately isSubtypeOf is running resolve for lazy types.
     // Because of this we can't run these assertions when we are creating this type. See EA-74904
     //
     // Also isSubtypeOf is not a very fast operation, so we are running assertions only if ASSERTIONS_ENABLED. See KT-7540
    private var assertionsDone = false

    private fun runAssertions() {
        if (assertionsDone || !ASSERTIONS_ENABLED) return
        assertionsDone = true

        assert (!lowerBound.isFlexible()) { "Lower bound of a flexible type can not be flexible: $lowerBound" }
        assert (!upperBound.isFlexible()) { "Upper bound of a flexible type can not be flexible: $upperBound" }
        assert (lowerBound != upperBound) { "Lower and upper bounds are equal: $lowerBound == $upperBound" }
        assert (KotlinTypeChecker.DEFAULT.isSubtypeOf(lowerBound, upperBound)) {
            "Lower bound $lowerBound of a flexible type must be a subtype of the upper bound $upperBound"
        }
    }

    override fun <T : TypeCapability> getCapability(capabilityClass: Class<T>): T? {
        val extra = extraCapabilities.getCapability(capabilityClass, this, this)
        if (extra != null) return extra

        @Suppress("UNCHECKED_CAST")
        if (capabilityClass in capabilityClasses) return this as T

        return super<DelegatingType>.getCapability(capabilityClass)
    }

    override fun makeNullableAsSpecified(nullable: Boolean): KotlinType {
        return create(
                TypeUtils.makeNullableAsSpecified(lowerBound, nullable),
                TypeUtils.makeNullableAsSpecified(upperBound, nullable),
                extraCapabilities)
    }

    override fun computeIsNullable() = delegateType.isMarkedNullable

    override fun isMarkedNullable(): Boolean = getCapability(NullAwareness::class.java)!!.computeIsNullable()

    override val delegateType: KotlinType
        get() {
            runAssertions()
            return lowerBound
        }

    override fun getDelegate() = getCapability(FlexibleTypeDelegation::class.java)!!.delegateType

    override fun toString() = "('$lowerBound'..'$upperBound')"
}
