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

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.load.java.lazy.LazyJavaPackageFragmentProvider
import org.jetbrains.kotlin.serialization.deserialization.DeserializationComponents
import org.jetbrains.kotlin.serialization.deserialization.LocalClassResolverImpl
import org.jetbrains.kotlin.storage.StorageManager

// This class is needed only for easier injection: exact types of needed components are specified in the constructor here.
// Otherwise injector generator is not smart enough to deduce, for example, which package fragment provider DeserializationComponents needs
public class DeserializationComponentsForJava(
        storageManager: StorageManager,
        moduleDescriptor: ModuleDescriptor,
        classDataFinder: JavaClassDataFinder,
        annotationAndConstantLoader: BinaryClassAnnotationAndConstantLoaderImpl,
        packageFragmentProvider: LazyJavaPackageFragmentProvider
) {
    val components: DeserializationComponents;

    {
        val localClassResolver = LocalClassResolverImpl()
        components = DeserializationComponents(
                storageManager, moduleDescriptor, classDataFinder, annotationAndConstantLoader, packageFragmentProvider,
                localClassResolver, JavaFlexibleTypeCapabilitiesDeserializer
        )
        localClassResolver.setDeserializationComponents(components)
    }
}
