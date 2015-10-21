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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.types.typeUtil.builtIns

open class DynamicTypesSettings {
    open val dynamicTypesAllowed: Boolean
        get() = false
}

class DynamicTypesAllowed: DynamicTypesSettings() {
    override val dynamicTypesAllowed: Boolean
        get() = true
}

interface Dynamicity : TypeCapability

fun KotlinType.isDynamic(): Boolean = this.getCapability(javaClass<Dynamicity>()) != null

fun createDynamicType(builtIns: KotlinBuiltIns) = object : DelegatingFlexibleType(
        builtIns.nothingType,
        builtIns.nullableAnyType,
        DynamicTypeCapabilities
) {}

public object DynamicTypeCapabilities : FlexibleTypeCapabilities {
    override val id: String get() = "kotlin.DynamicType"

    override fun <T : TypeCapability> getCapability(capabilityClass: Class<T>, jetType: KotlinType, flexibility: Flexibility): T? {
        @Suppress("UNCHECKED_CAST")
        return if (capabilityClass in Impl.capabilityClasses) Impl(flexibility) as T else null
    }

    private class Impl(flexibility: Flexibility) : Dynamicity, Specificity, NullAwareness, FlexibleTypeDelegation {
        companion object {
            internal val capabilityClasses = hashSetOf(
                    javaClass<Dynamicity>(),
                    javaClass<Specificity>(),
                    javaClass<NullAwareness>(),
                    javaClass<FlexibleTypeDelegation>()
            )
        }

        override val delegateType: KotlinType = flexibility.upperBound

        override fun getSpecificityRelationTo(otherType: KotlinType): Specificity.Relation {
            return if (!otherType.isDynamic()) Specificity.Relation.LESS_SPECIFIC else Specificity.Relation.DONT_KNOW
        }

        override fun makeNullableAsSpecified(nullable: Boolean): KotlinType {
            // Nullability has no effect on dynamics
            return createDynamicType(delegateType.builtIns)
        }

        override fun computeIsNullable() = false
    }
}
