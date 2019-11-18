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

package org.jetbrains.kotlin.load.java.sam

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.resolve.SamConversionResolver
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.SimpleType

class SamConversionResolverImpl(
        storageManager: StorageManager,
        private val samWithReceiverResolvers: Iterable<SamWithReceiverResolver>
): SamConversionResolver {
    private val functionTypesForSamInterfaces = storageManager.createCacheWithNullableValues<ClassDescriptor, SimpleType>()

    override fun resolveFunctionTypeIfSamInterface(classDescriptor: ClassDescriptor): SimpleType? {
        return functionTypesForSamInterfaces.computeIfAbsent(classDescriptor) {
            val abstractMethod = SingleAbstractMethodUtils.getSingleAbstractMethodOrNull(classDescriptor) ?: return@computeIfAbsent null
            val shouldConvertFirstParameterToDescriptor = samWithReceiverResolvers.any { it.shouldConvertFirstSamParameterToReceiver(abstractMethod) }
            SingleAbstractMethodUtils.getFunctionTypeForAbstractMethod(abstractMethod, shouldConvertFirstParameterToDescriptor)
        }
    }
}

object JavaBasedSamConversionResolver : SamConversionResolver {
    override fun resolveFunctionTypeIfSamInterface(classDescriptor: ClassDescriptor): SimpleType? {
        if (classDescriptor !is JavaClassDescriptor) return null
        return classDescriptor.defaultFunctionTypeForSamInterface
    }
}
