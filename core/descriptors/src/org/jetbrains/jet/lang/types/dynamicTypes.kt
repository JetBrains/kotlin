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

import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import kotlin.platform.platformStatic

open class DynamicTypesSettings {
    open val dynamicTypesAllowed: Boolean
        get() = false
}

class DynamicTypesAllowed: DynamicTypesSettings() {
    override val dynamicTypesAllowed: Boolean
        get() = true
}

trait Dynamicity : TypeCapability

object DynamicType : DelegatingFlexibleType(
        KotlinBuiltIns.getInstance().getNothingType(),
        KotlinBuiltIns.getInstance().getNullableAnyType(),
        DynamicTypeCapabilities
) {
    override fun getDelegate() = upperBound
}

fun JetType.isDynamic(): Boolean = this.getCapability(javaClass<Dynamicity>()) != null

public object DynamicTypeCapabilities : FlexibleTypeCapabilities {
    override val id: String get() = "kotlin.DynamicType"

    override fun <T : TypeCapability> getCapability(capabilityClass: Class<T>, jetType: JetType, flexibility: Flexibility): T? {
        if (capabilityClass.isAssignableFrom(javaClass<Impl>()))
            [suppress("UNCHECKED_CAST")]
            return Impl(jetType, flexibility) as T
        else return null
    }


    private class Impl(val type: JetType, val flexibility: Flexibility) : Dynamicity, Specificity {

        private val lowerBound: JetType get() = flexibility.lowerBound
        private val upperBound: JetType get() = flexibility.upperBound

        override fun getSpecificityRelationTo(otherType: JetType): Specificity.Relation {
            return if (!otherType.isDynamic()) Specificity.Relation.LESS_SPECIFIC else Specificity.Relation.DONT_KNOW
        }
    }
}
