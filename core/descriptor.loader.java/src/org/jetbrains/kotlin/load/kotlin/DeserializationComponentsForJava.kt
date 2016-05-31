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

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.java.lazy.LazyJavaPackageFragmentProvider
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.storage.StorageManager

// This class is needed only for easier injection: exact types of needed components are specified in the constructor here.
// Otherwise injector generator is not smart enough to deduce, for example, which package fragment provider DeserializationComponents needs
class DeserializationComponentsForJava(
        storageManager: StorageManager,
        moduleDescriptor: ModuleDescriptor,
        classDataFinder: JavaClassDataFinder,
        annotationAndConstantLoader: BinaryClassAnnotationAndConstantLoaderImpl,
        packageFragmentProvider: LazyJavaPackageFragmentProvider,
        notFoundClasses: NotFoundClasses,
        errorReporter: ErrorReporter,
        lookupTracker: LookupTracker
) {
    val components: DeserializationComponents

    init {
        val localClassResolver = LocalClassifierResolverImpl()
        val settings = JvmBuiltInsSettings(moduleDescriptor, storageManager, { moduleDescriptor })
        components = DeserializationComponents(
                storageManager, moduleDescriptor, classDataFinder, annotationAndConstantLoader, packageFragmentProvider, localClassResolver,
                errorReporter, lookupTracker, JavaFlexibleTypeDeserializer, ClassDescriptorFactory.EMPTY,
                notFoundClasses,
                additionalClassPartsProvider = settings,
                platformDependentDeclarationFilter = settings
        )
        localClassResolver.setDeserializationComponents(components)
    }
}
