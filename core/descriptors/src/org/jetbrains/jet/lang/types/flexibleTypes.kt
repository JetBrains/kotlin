/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.types

import org.jetbrains.jet.lang.types.checker.JetTypeChecker
import org.jetbrains.jet.lang.types.Approximation.DataFlowExtras
import org.jetbrains.jet.lang.resolve.name.FqName
import kotlin.platform.platformStatic
import org.jetbrains.jet.lang.resolve.name.ClassId
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe
import org.jetbrains.jet.lang.resolve.name.Name

public trait FlexibleTypeCapabilities {
    fun <T: TypeCapability> getCapability(capabilityClass: Class<T>, jetType: JetType, flexibility: Flexibility): T?
    val id: String

    object NONE : FlexibleTypeCapabilities {
        override fun <T : TypeCapability> getCapability(capabilityClass: Class<T>, jetType: JetType, flexibility: Flexibility): T? = null
        override val id: String get() = "NONE"
    }
}

public trait Flexibility : TypeCapability {
    class object {
        // This is a "magic" classifier: when type resolver sees it in the code, e.g. ft<Foo, Foo?>, instead of creating a normal type,
        // it creates a flexible type, e.g. (Foo..Foo?).
        // This is used in tests and Evaluate Expression to have flexible types in the code,
        // but normal users should not be referencing this classifier
        public val FLEXIBLE_TYPE_CLASSIFIER: ClassId = ClassId(FqName("kotlin.internal.flexible"), FqNameUnsafe.topLevel(Name.identifier("ft")))
    }

    // lowerBound is a subtype of upperBound
    public val lowerBound: JetType
    public val upperBound: JetType

    public val extraCapabilities: FlexibleTypeCapabilities
}

public fun JetType.isFlexible(): Boolean = this.getCapability(javaClass<Flexibility>()) != null
public fun JetType.flexibility(): Flexibility = this.getCapability(javaClass<Flexibility>())!!

// This function is intended primarily for sets: since JetType.equals() represents _syntactical_ equality of types,
// whereas JetTypeChecker.DEFAULT.equalsTypes() represents semantical equality
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
            candidate == other || JetTypeChecker.DEFAULT.equalTypes(candidate, other)
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
}

public trait Approximation : TypeCapability {
    public class Info(val from: JetType, val to: JetType, val message: String)
    public trait DataFlowExtras {
        object EMPTY : DataFlowExtras {
            override val canBeNull: Boolean get() = true
            override val possibleTypes: Set<JetType> get() = setOf()
            override val presentableText: String = "<unknown>"
        }

        class OnlyMessage(message: String) : DataFlowExtras {
            override val canBeNull: Boolean get() = true
            override val possibleTypes: Set<JetType> get() = setOf()
            override val presentableText: String = message
        }

        val canBeNull: Boolean
        val possibleTypes: Set<JetType>
        val presentableText: String
    }

    public fun approximateToExpectedType(expectedType: JetType, dataFlowExtras: DataFlowExtras): Info?
}

fun Approximation.Info.assertNotNull(): Boolean {
    return from.upperIfFlexible().isNullable() && !TypeUtils.isNullableType(to)
}

public fun JetType.getApproximationTo(
        expectedType: JetType,
        extras: Approximation.DataFlowExtras = Approximation.DataFlowExtras.EMPTY
): Approximation.Info? = this.getCapability(javaClass<Approximation>())?.approximateToExpectedType(expectedType, extras)


public open class DelegatingFlexibleType protected (
        override val lowerBound: JetType,
        override val upperBound: JetType,
        override val extraCapabilities: FlexibleTypeCapabilities
) : DelegatingType(), NullAwareness, Flexibility, Approximation {
    class object {
        platformStatic fun create(lowerBound: JetType, upperBound: JetType, extraCapabilities: FlexibleTypeCapabilities): JetType {
            if (lowerBound == upperBound) return lowerBound
            return DelegatingFlexibleType(lowerBound, upperBound, extraCapabilities)
        }
    }

    {
        assert (!lowerBound.isFlexible()) { "Lower bound of a flexible type can not be flexible: $lowerBound" }
        assert (!upperBound.isFlexible()) { "Upper bound of a flexible type can not be flexible: $upperBound" }
        assert (lowerBound != upperBound) { "Lower and upper bounds are equal: $lowerBound == $upperBound" }
        assert (JetTypeChecker.DEFAULT.isSubtypeOf(lowerBound, upperBound)) {
            "Lower bound $lowerBound of a flexible type must be a subtype of the upper bound $upperBound"
        }
    }

    override fun <T : TypeCapability> getCapability(capabilityClass: Class<T>): T? {
        return extraCapabilities.getCapability(capabilityClass, this, this) ?: super<DelegatingType>.getCapability(capabilityClass)
    }

    override fun makeNullableAsSpecified(nullable: Boolean): JetType {
        return create(
                TypeUtils.makeNullableAsSpecified(lowerBound, nullable),
                TypeUtils.makeNullableAsSpecified(upperBound, nullable),
                extraCapabilities)
    }

    override fun approximateToExpectedType(expectedType: JetType, dataFlowExtras: Approximation.DataFlowExtras): Approximation.Info? {
        // val foo: Any? = foo() : Foo!
        if (JetTypeChecker.DEFAULT.isSubtypeOf(upperBound, expectedType)) return null

        // if (foo : Foo! != null) {
        //     val bar: Any = foo
        // }
        if (!dataFlowExtras.canBeNull && JetTypeChecker.DEFAULT.isSubtypeOf(TypeUtils.makeNotNullable(upperBound), expectedType)) return null

        // TODO: maybe check possibleTypes to avoid extra approximations

        return Approximation.Info(this, expectedType, dataFlowExtras.presentableText)
    }

    override fun getDelegate() = lowerBound

    override fun toString() = "('$lowerBound'..'$upperBound')"
}
