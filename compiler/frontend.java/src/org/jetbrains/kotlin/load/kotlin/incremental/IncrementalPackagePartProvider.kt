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

package org.jetbrains.kotlin.load.kotlin.incremental

import org.jetbrains.kotlin.descriptors.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.ModuleMapping
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.storage.StorageManager

internal class IncrementalPackagePartProvider(
        private val parent: PackagePartProvider,
        incrementalCaches: List<IncrementalCache>,
        storageManager: StorageManager
) : PackagePartProvider {
    lateinit var deserializationConfiguration: DeserializationConfiguration

    private val moduleMappings = storageManager.createLazyValue {
        incrementalCaches.map { cache ->
            ModuleMapping.create(cache.getModuleMappingData(), "<incremental>", deserializationConfiguration)
        }
    }

    override fun findPackageParts(packageFqName: String): List<String> {
        return (moduleMappings().mapNotNull { it.findPackageParts(packageFqName) }.flatMap { it.parts } +
                parent.findPackageParts(packageFqName)).distinct()
    }

    // TODO
    override fun findMetadataPackageParts(packageFqName: String): List<String> = TODO()
}
