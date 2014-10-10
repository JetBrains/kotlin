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

public trait FlexibleTypeCapabilities {
    fun <T: TypeCapability> getCapability(capabilityClass: Class<T>, jetType: JetType, flexibility: Flexibility): T?
    val id: String

    object NONE : FlexibleTypeCapabilities {
        override fun <T : TypeCapability> getCapability(capabilityClass: Class<T>, jetType: JetType, flexibility: Flexibility): T? = null
        override val id: String get() = "NONE"
    }
}

public trait Flexibility : TypeCapability {
    // lowerBound is a subtype of upperBound
    public fun getUpperBound(): JetType

    public fun getLowerBound(): JetType

    public fun getExtraCapabilities(): FlexibleTypeCapabilities
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

public fun JetType.lowerIfFlexible(): JetType = if (this.isFlexible()) this.flexibility().getLowerBound() else this
public fun JetType.upperIfFlexible(): JetType = if (this.isFlexible()) this.flexibility().getUpperBound() else this

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
        private val _lowerBound: JetType,
        private val _upperBound: JetType,
        private val _extraCapabilities: FlexibleTypeCapabilities
) : DelegatingType(), NullAwareness, Flexibility, Approximation {
    class object {
        fun create(lowerBound: JetType, upperBound: JetType, extraCapabilities: FlexibleTypeCapabilities): JetType {
            if (lowerBound == upperBound) return lowerBound
            return DelegatingFlexibleType(lowerBound, upperBound, extraCapabilities)
        }
    }

    {
        assert (!_lowerBound.isFlexible()) { "Lower bound of a flexible type can not be flexible: $_lowerBound" }
        assert (!_upperBound.isFlexible()) { "Upper bound of a flexible type can not be flexible: $_upperBound" }
        assert (_lowerBound != _upperBound) { "Lower and upper bounds are equal: $_lowerBound == $_upperBound" }
        assert (JetTypeChecker.DEFAULT.isSubtypeOf(_lowerBound, _upperBound)) {
            "Lower bound $_lowerBound of a flexible type must be a subtype of the upper bound $_upperBound"
        }
    }

    override fun getUpperBound(): JetType = _upperBound
    override fun getLowerBound(): JetType = _lowerBound

    override fun getExtraCapabilities() = _extraCapabilities

    override fun <T : TypeCapability> getCapability(capabilityClass: Class<T>): T? {
        return getExtraCapabilities().getCapability(capabilityClass, this, this) ?: super<DelegatingType>.getCapability(capabilityClass)
    }

    override fun makeNullableAsSpecified(nullable: Boolean): JetType {
        return create(
                TypeUtils.makeNullableAsSpecified(_lowerBound, nullable),
                TypeUtils.makeNullableAsSpecified(_upperBound, nullable),
                getExtraCapabilities())
    }

    override fun approximateToExpectedType(expectedType: JetType, dataFlowExtras: Approximation.DataFlowExtras): Approximation.Info? {
        // val foo: Any? = foo() : Foo!
        if (JetTypeChecker.DEFAULT.isSubtypeOf(getUpperBound(), expectedType)) return null

        // if (foo : Foo! != null) {
        //     val bar: Any = foo
        // }
        if (!dataFlowExtras.canBeNull && JetTypeChecker.DEFAULT.isSubtypeOf(TypeUtils.makeNotNullable(getUpperBound()), expectedType)) return null

        // TODO: maybe check possibleTypes to avoid extra approximations

        return Approximation.Info(this, expectedType, dataFlowExtras.presentableText)
    }

    override fun getDelegate() = _lowerBound

    override fun toString() = "('$_lowerBound'..'$_upperBound')"
}
