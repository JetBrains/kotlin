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

import org.jetbrains.kotlin.load.kotlin.JvmPackagePartProviderBase
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.load.kotlin.loadModuleMapping
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.deserialization.ClassData
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration

class IncrementalPackagePartProvider(
    private val parent: PackagePartProvider,
    incrementalCaches: List<IncrementalCache>
) : PackagePartProvider {

    lateinit var deserializationConfiguration: DeserializationConfiguration

    init {
        (parent as? JvmPackagePartProviderBase<*>)?.deserializationConfiguration?.let {
            deserializationConfiguration = it
        }
    }

    private val moduleMappings by lazy {
        incrementalCaches.map { cache ->
            ModuleMapping.loadModuleMapping(cache.getModuleMappingData(), "<incremental>", deserializationConfiguration) { version ->
                // Incremental compilation should fall back to full rebuild if the minor component of the metadata version has changed
                throw IllegalStateException("Version of the generated module should not be incompatible: $version")
            }
        }
    }

    override fun findPackageParts(packageFqName: String): List<String> {
        return (moduleMappings.mapNotNull { it.findPackageParts(packageFqName) }.flatMap { it.parts } +
                parent.findPackageParts(packageFqName)).distinct()
    }

    override fun getAnnotationsOnBinaryModule(moduleName: String): List<ClassId> {
        return parent.getAnnotationsOnBinaryModule(moduleName)
    }

    override fun getAllOptionalAnnotationClasses(): List<ClassData> =
        moduleMappings.flatMap((JvmPackagePartProviderBase)::getAllOptionalAnnotationClasses) +
                parent.getAllOptionalAnnotationClasses()
}
