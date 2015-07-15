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
import org.jetbrains.kotlin.types.checker.JetTypeChecker
import kotlin.platform.platformStatic

public trait FlexibleTypeCapabilities {
    fun <T: TypeCapability> getCapability(capabilityClass: Class<T>, jetType: JetType, flexibility: Flexibility): T?
    val id: String

    object NONE : FlexibleTypeCapabilities {
        override fun <T : TypeCapability> getCapability(capabilityClass: Class<T>, jetType: JetType, flexibility: Flexibility): T? = null
        override val id: String get() = "NONE"
    }
}

public trait Flexibility : TypeCapability, SubtypingRepresentatives {
    companion object {
        // This is a "magic" classifier: when type resolver sees it in the code, e.g. ft<Foo, Foo?>, instead of creating a normal type,
        // it creates a flexible type, e.g. (Foo..Foo?).
        // This is used in tests and Evaluate Expression to have flexible types in the code,
        // but normal users should not be referencing this classifier
        public val FLEXIBLE_TYPE_CLASSIFIER: ClassId = ClassId.topLevel(FqName("kotlin.internal.flexible.ft"))
    }

    // lowerBound is a subtype of upperBound
    public val lowerBound: JetType
    public val upperBound: JetType

    public val extraCapabilities: FlexibleTypeCapabilities

    override val subTypeRepresentative: JetType
        get() = lowerBound

    override val superTypeRepresentative: JetType
        get() = upperBound

    override fun sameTypeConstructor(type: JetType) = false
}

public fun JetType.isFlexible(): Boolean = this.getCapability(javaClass<Flexibility>()) != null
public fun JetType.flexibility(): Flexibility = this.getCapability(javaClass<Flexibility>())!!

public fun JetType.isNullabilityFlexible(): Boolean {
    val flexibility = this.getCapability(javaClass<Flexibility>()) ?: return false
    return TypeUtils.isNullableType(flexibility.lowerBound) != TypeUtils.isNullableType(flexibility.upperBound)
}

// This function is intended primarily for sets: since JetType.equals() represents _syntactical_ equality of types,
// whereas JetTypeChecker.DEFAULT.equalsTypes() represents semantic equality
// A set of types (e.g. exact bounds etc) may contain, for example, X, X? and X!
// These are not equal syntactically (by JetType.equals()), but X! is _compatible_ with others as exact bounds,
// moreover, X! is a better fit.
//
// So, we are looking for a type among this set such that it is equal to all others semantically
// (by JetTypeChecker.DEFAULT.equalsTypes()), and fits at least as well as they do.
fun Collection<JetType>.singleBestRepresentative(): JetType? {
    if (this.size() == 1) return this.first()

    return this.firstOrNull {
        candidate ->
        this.all {
            other ->
            // We consider error types equal to anything here, so that intersections like
            // {Array<String>, Array<[ERROR]>} work correctly
            candidate == other || JetTypeChecker.ERROR_TYPES_ARE_EQUAL_TO_ANYTHING.equalTypes(candidate, other)
        }
    }
}

fun Collection<TypeProjection>.singleBestRepresentative(): TypeProjection? {
    if (this.size() == 1) return this.first()

    val projectionKinds = this.map { it.getProjectionKind() }.toSet()
    if (projectionKinds.size() != 1) return null

    val bestType = this.map { it.getType() }.singleBestRepresentative()
    if (bestType == null) return null

    return TypeProjectionImpl(projectionKinds.single(), bestType)
}

public fun JetType.lowerIfFlexible(): JetType = if (this.isFlexible()) this.flexibility().lowerBound else this
public fun JetType.upperIfFlexible(): JetType = if (this.isFlexible()) this.flexibility().upperBound else this

public trait NullAwareness : TypeCapability {
    public fun makeNullableAsSpecified(nullable: Boolean): JetType
    public fun computeIsNullable(): Boolean
}

trait FlexibleTypeDelegation : TypeCapability {
    public val delegateType: JetType
}

public open class DelegatingFlexibleType protected constructor(
        override val lowerBound: JetType,
        override val upperBound: JetType,
        override val extraCapabilities: FlexibleTypeCapabilities
) : DelegatingType(), NullAwareness, Flexibility, FlexibleTypeDelegation {
    companion object {
        internal val capabilityClasses = hashSetOf(
                javaClass<NullAwareness>(),
                javaClass<Flexibility>(),
                javaClass<SubtypingRepresentatives>(),
                javaClass<FlexibleTypeDelegation>()
        )

        platformStatic fun create(lowerBound: JetType, upperBound: JetType, extraCapabilities: FlexibleTypeCapabilities): JetType {
            if (lowerBound == upperBound) return lowerBound
            return DelegatingFlexibleType(lowerBound, upperBound, extraCapabilities)
        }
    }

    init {
        if (ASSERTIONS_ENABLED) { // workaround for KT-7540
            assert (!lowerBound.isFlexible()) { "Lower bound of a flexible type can not be flexible: $lowerBound" }
            assert (!upperBound.isFlexible()) { "Upper bound of a flexible type can not be flexible: $upperBound" }
            assert (lowerBound != upperBound) { "Lower and upper bounds are equal: $lowerBound == $upperBound" }
            assert (JetTypeChecker.DEFAULT.isSubtypeOf(lowerBound, upperBound)) {
                "Lower bound $lowerBound of a flexible type must be a subtype of the upper bound $upperBound"
            }
        }
    }

    override fun <T : TypeCapability> getCapability(capabilityClass: Class<T>): T? {
        val extra = extraCapabilities.getCapability(capabilityClass, this, this)
        if (extra != null) return extra

        @suppress("UNCHECKED_CAST")
        if (capabilityClass in capabilityClasses) return this as T

        return super<DelegatingType>.getCapability(capabilityClass)
    }

    override fun makeNullableAsSpecified(nullable: Boolean): JetType {
        return create(
                TypeUtils.makeNullableAsSpecified(lowerBound, nullable),
                TypeUtils.makeNullableAsSpecified(upperBound, nullable),
                extraCapabilities)
    }

    override fun computeIsNullable() = delegateType.isMarkedNullable()

    override fun isMarkedNullable(): Boolean = getCapability(javaClass<NullAwareness>())!!.computeIsNullable()

    override val delegateType: JetType = lowerBound

    override fun getDelegate() = getCapability(javaClass<FlexibleTypeDelegation>())!!.delegateType

    override fun toString() = "('$lowerBound'..'$upperBound')"
}
