/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.js.lower

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.name.Name

class InnerClassDescriptorProvider {
    private val outerPropertyDescriptors = mutableMapOf<ClassDescriptor, PropertyDescriptor>()
    private val constructorDescriptors = mutableMapOf<ClassConstructorDescriptor, ClassConstructorDescriptor>()

    fun getOuterProperty(cls: ClassDescriptor): PropertyDescriptor {
        assert(cls.isInner) { "Outer property is not available for non-inner class $cls" }

        return outerPropertyDescriptors.getOrPut(cls) {
            val property = PropertyDescriptorImpl.create(
                    cls, Annotations.EMPTY, Modality.FINAL, Visibilities.PRIVATE, true, Name.identifier("\$outer"),
                    CallableMemberDescriptor.Kind.SYNTHESIZED, cls.source,
                    false, false, false, false, false, false)
            val outerClass = cls.containingDeclaration as ClassDescriptor
            property.setOutType(outerClass.defaultType)
            property
        }
    }

    fun getConstructor(ctor: ClassConstructorDescriptor): ClassConstructorDescriptor {
        return constructorDescriptors.getOrPut(ctor) {
            val newCtor = ClassConstructorDescriptorImpl.createSynthesized(
                    ctor.constructedClass, ctor.annotations, ctor.isPrimary, ctor.source)
            val outerClass = ctor.constructedClass.containingDeclaration as ClassDescriptor

            val outerParam = ValueParameterDescriptorImpl(
                    newCtor, null, 0, Annotations.EMPTY, Name.identifier("\$outer"), outerClass.defaultType, false,
                    false, false, null, ctor.source)
            val newParams = ctor.valueParameters.map { param ->
                ValueParameterDescriptorImpl(
                        newCtor, null, param.index + 1, param.annotations, param.name, param.type, param.declaresDefaultValue(),
                        param.isCrossinline, param.isNoinline, param.varargElementType, param.source)
            }
            newCtor.initialize(listOf(outerParam) + newParams, ctor.visibility)
            newCtor.returnType = ctor.returnType
            newCtor
        }
    }
}