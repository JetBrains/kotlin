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
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.load.java.lazy.types.RawSubstitution
import org.jetbrains.kotlin.resolve.ExternalOverridabilityCondition
import org.jetbrains.kotlin.resolve.ExternalOverridabilityCondition.Result
import org.jetbrains.kotlin.resolve.OverridingUtil

class ErasedOverridabilityCondition : ExternalOverridabilityCondition {
    override fun isOverridable(superDescriptor: CallableDescriptor, subDescriptor: CallableDescriptor): Result {
        if (subDescriptor !is JavaMethodDescriptor) return Result.UNKNOWN

        var erasedSuper = superDescriptor.substitute(RawSubstitution.buildSubstitutor()) ?: return Result.UNKNOWN

        if (erasedSuper is SimpleFunctionDescriptor && erasedSuper.typeParameters.isNotEmpty()) {
            // Only simple functions are supported now for erased overrides
            erasedSuper = erasedSuper.createCopyWithNewTypeParameters(emptyList())
        }

        val overridabilityResult =
                OverridingUtil.DEFAULT.isOverridableByWithoutExternalConditions(erasedSuper, subDescriptor, false).result
        return when (overridabilityResult) {
            OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE -> Result.OVERRIDABLE
            else ->  Result.UNKNOWN
        }
    }
}
