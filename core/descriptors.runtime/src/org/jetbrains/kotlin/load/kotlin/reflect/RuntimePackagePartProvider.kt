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

package org.jetbrains.kotlin.load.kotlin.reflect

import org.jetbrains.kotlin.descriptors.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.ModuleMapping
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import java.util.concurrent.ConcurrentHashMap

class RuntimePackagePartProvider(private val classLoader: ClassLoader) : PackagePartProvider {
    private val module2Mapping = ConcurrentHashMap<String, ModuleMapping>()

    fun registerModule(moduleName: String) {
        val mapping = try {
            val resourcePath = "META-INF/$moduleName.${ModuleMapping.MAPPING_FILE_EXT}"
            classLoader.getResourceAsStream(resourcePath)?.use { stream ->
                ModuleMapping.create(stream.readBytes(), resourcePath, DeserializationConfiguration.Default)
            }
        }
        catch (e: Exception) {
            // TODO: do not swallow this exception?
            null
        }
        module2Mapping.putIfAbsent(moduleName, mapping ?: ModuleMapping.EMPTY)
    }

    override fun findPackageParts(packageFqName: String): List<String> {
        return module2Mapping.values.mapNotNull { it.findPackageParts(packageFqName) }.flatMap { it.parts }.distinct()
    }

    // TODO
    override fun findMetadataPackageParts(packageFqName: String): List<String> = TODO()
}
