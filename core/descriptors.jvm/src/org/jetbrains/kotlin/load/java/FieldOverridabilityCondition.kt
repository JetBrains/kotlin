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

package org.jetbrains.kotlin.load.java

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.load.java.lazy.descriptors.isJavaField
import org.jetbrains.kotlin.resolve.ExternalOverridabilityCondition
import org.jetbrains.kotlin.resolve.ExternalOverridabilityCondition.Result

class FieldOverridabilityCondition : ExternalOverridabilityCondition {
    override fun isOverridable(
        superDescriptor: CallableDescriptor,
        subDescriptor: CallableDescriptor,
        subClassDescriptor: ClassDescriptor?
    ): Result {
        if (subDescriptor !is PropertyDescriptor || superDescriptor !is PropertyDescriptor) return Result.UNKNOWN
        if (subDescriptor.name != superDescriptor.name) return Result.UNKNOWN

        if (subDescriptor.isJavaField && superDescriptor.isJavaField) return Result.OVERRIDABLE
        if (subDescriptor.isJavaField || superDescriptor.isJavaField) return Result.INCOMPATIBLE

        return Result.UNKNOWN
    }

    override fun getContract() = ExternalOverridabilityCondition.Contract.BOTH
}
