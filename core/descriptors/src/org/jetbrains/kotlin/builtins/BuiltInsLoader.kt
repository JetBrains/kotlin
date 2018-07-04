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

package org.jetbrains.kotlin.builtins

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.deserialization.AdditionalClassPartsProvider
import org.jetbrains.kotlin.descriptors.deserialization.ClassDescriptorFactory
import org.jetbrains.kotlin.descriptors.deserialization.PlatformDependentDeclarationFilter
import org.jetbrains.kotlin.storage.StorageManager
import java.util.*

interface BuiltInsLoader {
    fun createPackageFragmentProvider(
            storageManager: StorageManager,
            builtInsModule: ModuleDescriptor,
            classDescriptorFactories: Iterable<ClassDescriptorFactory>,
            platformDependentDeclarationFilter: PlatformDependentDeclarationFilter,
            additionalClassPartsProvider: AdditionalClassPartsProvider
    ): PackageFragmentProvider

    companion object {
        val Instance: BuiltInsLoader by lazy(LazyThreadSafetyMode.PUBLICATION) {
            val implementations = ServiceLoader.load(BuiltInsLoader::class.java, BuiltInsLoader::class.java.classLoader)
            implementations.firstOrNull() ?: throw IllegalStateException(
                "No BuiltInsLoader implementation was found. Please ensure that the META-INF/services/ is not stripped " +
                "from your application and that the Java virtual machine is not running under a security manager"
            )
        }
    }
}
