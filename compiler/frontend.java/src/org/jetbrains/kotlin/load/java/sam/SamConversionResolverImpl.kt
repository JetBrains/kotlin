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

package org.jetbrains.kotlin.load.java.sam

import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.load.java.components.SamConversionResolver
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassConstructorDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.load.java.descriptors.SamConstructorDescriptor
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaClassDescriptor
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.SimpleType

class SamConversionResolverImpl(val storageManager: StorageManager, val samWithReceiverResolver: SamWithReceiverResolver): SamConversionResolver {
    override fun resolveSamConstructor(constructorOwner: DeclarationDescriptor, classifier: () -> ClassifierDescriptor?): SamConstructorDescriptor? {
        val classifierDescriptor = classifier()
        if (classifierDescriptor !is LazyJavaClassDescriptor || classifierDescriptor.functionTypeForSamInterface == null) return null
        return SingleAbstractMethodUtils.createSamConstructorFunction(constructorOwner, classifierDescriptor)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <D : FunctionDescriptor> resolveSamAdapter(original: D): D? {
        return when {
            !SingleAbstractMethodUtils.isSamAdapterNecessary(original) -> null
            original is JavaClassConstructorDescriptor -> SingleAbstractMethodUtils.createSamAdapterConstructor(original) as D
            original is JavaMethodDescriptor -> SingleAbstractMethodUtils.createSamAdapterFunction(original) as D
            else -> null
        }
    }

    private val functionTypesForSamInterfaces = storageManager.createCacheWithNullableValues<JavaClassDescriptor, SimpleType>()

    override fun resolveFunctionTypeIfSamInterface(classDescriptor: JavaClassDescriptor): SimpleType? {
        return functionTypesForSamInterfaces.computeIfAbsent(classDescriptor) {
            val abstractMethod = SingleAbstractMethodUtils.getSingleAbstractMethodOrNull(classDescriptor) ?: return@computeIfAbsent null
            val shouldConvertFirstParameterToDescriptor = samWithReceiverResolver.shouldConvertFirstSamParameterToReceiver(abstractMethod)
            SingleAbstractMethodUtils.getFunctionTypeForAbstractMethod(abstractMethod, shouldConvertFirstParameterToDescriptor)
        }
    }
}
