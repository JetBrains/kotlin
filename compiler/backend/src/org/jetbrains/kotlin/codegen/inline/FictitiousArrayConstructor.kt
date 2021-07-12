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

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl

internal class FictitiousArrayConstructor(arrayClass: ClassDescriptor) : SimpleFunctionDescriptorImpl(
    arrayClass.containingDeclaration, null, Annotations.EMPTY, arrayClass.name, CallableMemberDescriptor.Kind.SYNTHESIZED,
    SourceElement.NO_SOURCE
) {
    companion object Factory {
        @JvmStatic
        fun create(arrayConstructor: ConstructorDescriptor): FictitiousArrayConstructor {
            val arrayClass = arrayConstructor.constructedClass
            return FictitiousArrayConstructor(arrayClass).apply {
                this.initialize(
                    null, null, emptyList<ReceiverParameterDescriptor>(), arrayConstructor.typeParameters, arrayConstructor.valueParameters, arrayClass.defaultType,
                    Modality.FINAL, DescriptorVisibilities.PUBLIC
                )
                this.isInline = true
            }
        }
    }
}
