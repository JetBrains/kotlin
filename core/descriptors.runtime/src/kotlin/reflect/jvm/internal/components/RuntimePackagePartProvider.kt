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

package kotlin.reflect.jvm.internal.components

import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.loadModuleMapping
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import java.io.IOException
import java.util.*

class RuntimePackagePartProvider(private val classLoader: ClassLoader) : PackagePartProvider {
    // Names of modules which were registered with registerModule
    private val visitedModules = hashSetOf<String>()

    // Package FQ name -> list of JVM internal names of package parts in that package across all registered modules
    private val packageParts = hashMapOf<String, LinkedHashSet<String>>()

    @Synchronized
    fun registerModule(moduleName: String) {
        if (!visitedModules.add(moduleName)) return

        val resourcePath = "META-INF/$moduleName.${ModuleMapping.MAPPING_FILE_EXT}"
        val resources = try {
            classLoader.getResources(resourcePath)
        } catch (e: IOException) {
            EmptyEnumeration
        }

        for (resource in resources) {
            try {
                resource.openStream()?.use { stream ->
                    val mapping = ModuleMapping.loadModuleMapping(
                        stream.readBytes(), resourcePath, DeserializationConfiguration.Default
                    ) { version ->
                        throw UnsupportedOperationException(
                            "Module was compiled with an incompatible version of Kotlin. The binary version of its metadata is $version, " +
                                    "expected version is ${JvmMetadataVersion.INSTANCE}. Please update Kotlin to the latest version"
                        )
                    }
                    for ((packageFqName, parts) in mapping.packageFqName2Parts) {
                        packageParts.getOrPut(packageFqName) { linkedSetOf() }.addAll(parts.parts)
                    }
                }
            } catch (e: UnsupportedOperationException) {
                throw e
            } catch (e: Exception) {
                // TODO: do not swallow this exception?
            }
        }
    }

    @Synchronized
    override fun findPackageParts(packageFqName: String): List<String> =
        packageParts[packageFqName]?.toList().orEmpty()

    override fun getAnnotationsOnBinaryModule(moduleName: String): List<ClassId> {
        // TODO: load annotations from resource files
        return emptyList()
    }

    private object EmptyEnumeration : Enumeration<Nothing> {
        override fun hasMoreElements(): Boolean = false
        override fun nextElement(): Nothing = throw NoSuchElementException()
    }
}
