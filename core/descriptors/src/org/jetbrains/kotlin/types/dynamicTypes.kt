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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.builtIns

open class DynamicTypesSettings {
    open val dynamicTypesAllowed: Boolean
        get() = false
}

class DynamicTypesAllowed: DynamicTypesSettings() {
    override val dynamicTypesAllowed: Boolean
        get() = true
}

fun KotlinType.isDynamic(): Boolean = this.getCapability(Flexibility::class.java)?.factory == DynamicTypeFactory

fun createDynamicType(builtIns: KotlinBuiltIns) = DynamicType(builtIns)

class DynamicType(builtIns: KotlinBuiltIns) : DelegatingFlexibleType(builtIns.nothingType, builtIns.nullableAnyType, DynamicTypeFactory) {
    override val delegateType: KotlinType get() = upperBound

    override fun makeNullableAsSpecified(nullable: Boolean): KotlinType {
        // Nullability has no effect on dynamics
        return createDynamicType(delegateType.builtIns)
    }

    override val isMarkedNullable: Boolean get() = false
}

object DynamicTypeFactory : FlexibleTypeFactory {
    override fun create(lowerBound: SimpleType, upperBound: SimpleType): KotlinType {
        if (KotlinTypeChecker.FLEXIBLE_UNEQUAL_TO_INFLEXIBLE.equalTypes(lowerBound, lowerBound.builtIns.nothingType) &&
            KotlinTypeChecker.FLEXIBLE_UNEQUAL_TO_INFLEXIBLE.equalTypes(upperBound, upperBound.builtIns.nullableAnyType)) {
            return createDynamicType(lowerBound.builtIns)
        }
        else {
            throw IllegalStateException("Illegal type range for dynamic type: $lowerBound..$upperBound")
        }
    }


    override val id: String get() = "kotlin.DynamicType"
}
