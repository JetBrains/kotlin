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

public trait Flexibility : TypeCapability {
    // lowerBound is a subtype of upperBound
    public fun getUpperBound(): JetType

    public fun getLowerBound(): JetType
}

public fun JetType.isFlexible(): Boolean = this.getCapability(javaClass<Flexibility>()) != null
public fun JetType.flexibility(): Flexibility = this.getCapability(javaClass<Flexibility>())!!

public fun JetType.lowerIfFlexible(): JetType = if (this.isFlexible()) this.flexibility().getLowerBound() else this

public trait NullAwareness : TypeCapability {
    public fun makeNullableAsSpecified(nullable: Boolean): JetType
}

public open class DelegatingFlexibleType protected (
        private val _lowerBound: JetType,
        private val _upperBound: JetType
) : DelegatingType(), NullAwareness, Flexibility {
    class object {
        public fun create(lowerBound: JetType, upperBound: JetType): JetType {
            if (lowerBound == upperBound) return lowerBound
            return DelegatingFlexibleType(lowerBound, upperBound)
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

    protected open fun create(lowerBound: JetType, upperBound: JetType): JetType {
        return DelegatingFlexibleType.create(lowerBound, upperBound)
    }

    override fun makeNullableAsSpecified(nullable: Boolean): JetType {
        return create(TypeUtils.makeNullableAsSpecified(_lowerBound, nullable), TypeUtils.makeNullableAsSpecified(_upperBound, nullable))
    }

    override fun getDelegate() = _lowerBound

    override fun toString() = "('$_lowerBound'..'$_upperBound')"
}
