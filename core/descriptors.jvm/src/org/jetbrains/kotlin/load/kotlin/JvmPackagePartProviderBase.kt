/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping
import org.jetbrains.kotlin.metadata.jvm.deserialization.PackageParts
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.deserialization.ClassData
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.serialization.deserialization.MetadataPartProvider

abstract class JvmPackagePartProviderBase<MappingsKey> : PackagePartProvider, MetadataPartProvider {

    protected data class ModuleMappingInfo<MappingsKey>(val key: MappingsKey, val mapping: ModuleMapping, val name: String)

    protected abstract val loadedModules: MutableList<ModuleMappingInfo<MappingsKey>>

    abstract val deserializationConfiguration : DeserializationConfiguration

    override fun findPackageParts(packageFqName: String): List<String> {
        val rootToPackageParts: Collection<PackageParts> = getPackageParts(packageFqName)
        if (rootToPackageParts.isEmpty()) return emptyList()

        val result = linkedSetOf<String>()
        val visitedMultifileFacades = linkedSetOf<String>()
        for (packageParts in rootToPackageParts) {
            for (name in packageParts.parts) {
                val facadeName = packageParts.getMultifileFacadeName(name)
                if (facadeName == null || facadeName !in visitedMultifileFacades) {
                    result.add(name)
                }
            }
            packageParts.parts.mapNotNullTo(visitedMultifileFacades, packageParts::getMultifileFacadeName)
        }
        return result.toList()
    }

    override fun findMetadataPackageParts(packageFqName: String): List<String> =
        getPackageParts(packageFqName).flatMap(PackageParts::metadataParts).distinct()

    private fun getPackageParts(packageFqName: String): Collection<PackageParts> {
        val result = mutableMapOf<MappingsKey, PackageParts>()
        for ((root, mapping) in loadedModules) {
            val newParts = mapping.findPackageParts(packageFqName) ?: continue
            result[root]?.let { parts -> parts += newParts } ?: result.put(root, newParts)
        }
        return result.values
    }

    override fun getAnnotationsOnBinaryModule(moduleName: String): List<ClassId> {
        return loadedModules.mapNotNull { (_, mapping, name) ->
            if (name == moduleName) mapping.moduleData.annotations.map(ClassId::fromString) else null
        }.flatten()
    }

    override fun getAllOptionalAnnotationClasses(): List<ClassData> =
        loadedModules.flatMap { module ->
            getAllOptionalAnnotationClasses(module.mapping)
        }

    companion object {
        fun getAllOptionalAnnotationClasses(module: ModuleMapping): List<ClassData> {
            val data = module.moduleData
            return data.optionalAnnotations.map { proto ->
                ClassData(data.nameResolver, proto, module.version, SourceElement.NO_SOURCE)
            }
        }
    }
}
