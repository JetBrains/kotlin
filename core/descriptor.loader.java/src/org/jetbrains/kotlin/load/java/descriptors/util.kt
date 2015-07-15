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

package org.jetbrains.kotlin.load.java.descriptors

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.JetType

fun createEnhancedValueParameters(
        enhancedTypes: Collection<JetType>,
        oldValueParameters: Collection<ValueParameterDescriptor>,
        newOwner: CallableDescriptor
): List<ValueParameterDescriptor> {
    assert(enhancedTypes.size() == oldValueParameters.size()) {
        "Different value parameters sizes: Enhanced = ${enhancedTypes.size()}, Old = ${oldValueParameters.size()}"
    }

    return enhancedTypes.zip(oldValueParameters).map {
        pair ->
        val (newType, oldParameter) = pair
        ValueParameterDescriptorImpl(
                newOwner,
                null,
                oldParameter.getIndex(),
                oldParameter.getAnnotations(),
                oldParameter.getName(),
                newType,
                oldParameter.declaresDefaultValue(),
                if (oldParameter.getVarargElementType() != null) newOwner.module.builtIns.getArrayElementType(newType) else null,
                oldParameter.getSource()
        )
    }
}
