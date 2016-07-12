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

package org.jetbrains.kotlin.load.java.components

import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.descriptors.SamConstructorDescriptor
import org.jetbrains.kotlin.types.SimpleType

interface SamConversionResolver {
    companion object EMPTY : SamConversionResolver {
        override fun <D : FunctionDescriptor> resolveSamAdapter(original: D) = null
        override fun resolveSamConstructor(constructorOwner: DeclarationDescriptor, classifier: () -> ClassifierDescriptor?) = null
        override fun resolveFunctionTypeIfSamInterface(classDescriptor: JavaClassDescriptor): SimpleType? = null
    }

    fun resolveSamConstructor(constructorOwner: DeclarationDescriptor, classifier: () -> ClassifierDescriptor?): SamConstructorDescriptor?

    fun <D : FunctionDescriptor> resolveSamAdapter(original: D): D?

    fun resolveFunctionTypeIfSamInterface(classDescriptor: JavaClassDescriptor): SimpleType?
}
