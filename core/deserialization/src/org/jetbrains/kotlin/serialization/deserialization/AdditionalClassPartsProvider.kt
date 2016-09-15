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

package org.jetbrains.kotlin.serialization.deserialization

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.types.KotlinType

interface AdditionalClassPartsProvider {
    fun getSupertypes(classDescriptor: DeserializedClassDescriptor): Collection<KotlinType>
    fun getFunctions(name: Name, classDescriptor: DeserializedClassDescriptor): Collection<SimpleFunctionDescriptor>
    fun getConstructors(classDescriptor: DeserializedClassDescriptor): Collection<ClassConstructorDescriptor>
    fun getFunctionsNames(classDescriptor: DeserializedClassDescriptor): Collection<Name>

    object None : AdditionalClassPartsProvider {
        override fun getSupertypes(classDescriptor: DeserializedClassDescriptor): Collection<KotlinType> = emptyList()
        override fun getFunctions(name: Name, classDescriptor: DeserializedClassDescriptor): Collection<SimpleFunctionDescriptor> = emptyList()
        override fun getFunctionsNames(classDescriptor: DeserializedClassDescriptor): Collection<Name> = emptyList()
        override fun getConstructors(classDescriptor: DeserializedClassDescriptor): Collection<ClassConstructorDescriptor> = emptyList()
    }
}
