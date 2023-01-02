/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.backend.common.output.OutputFile
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.load.kotlin.loadModuleMapping
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.jvm.JvmModuleProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping
import org.jetbrains.kotlin.metadata.jvm.deserialization.PackageParts
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.serialization.KotlinSerializerExtensionBase
import org.jetbrains.kotlin.serialization.StringTableImpl
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol

fun ClassFileFactory.getClassFiles(): Iterable<OutputFile> {
    return asList().filterClassFiles()
}

fun List<OutputFile>.filterClassFiles(): List<OutputFile> {
    return filter { it.relativePath.endsWith(".class") }
}

fun JvmModuleProtoBuf.Module.Builder.addDataFromCompiledModule(
    registry: PackagePartRegistry, stringTable: StringTableImpl, state: GenerationState
) {
    for (part in registry.parts.values.addCompiledPartsAndSort(state)) {
        part.addTo(this)
    }

    // Take all optional annotation classes from sources, as well as look up all previously compiled optional annotation classes
    // by FQ name in the current module. The latter is needed because in incremental compilation scenario, the already compiled
    // classes will not be available via sources.
    val optionalAnnotationClassDescriptors =
        registry.optionalAnnotations.toSet() +
                state.loadCompiledModule()?.moduleData?.run {
                    optionalAnnotations.mapNotNull { proto ->
                        state.module.findClassAcrossModuleDependencies(
                            ClassId.fromString(nameResolver.getQualifiedClassName(proto.fqName))
                        )
                    }
                }.orEmpty()

    val serializer = DescriptorSerializer.createTopLevel(
        JvmOptionalAnnotationSerializerExtension(stringTable), state.configuration.languageVersionSettings,
    )
    for (descriptor in optionalAnnotationClassDescriptors) {
        addOptionalAnnotationClass(serializer.classProto(descriptor))
    }
}

class JvmOptionalAnnotationSerializerExtension(
    override val stringTable: StringTableImpl
) : KotlinSerializerExtensionBase(BuiltInSerializerProtocol) {
    override val metadataVersion: BinaryVersion
        get() = JvmMetadataVersion.INSTANCE

    override fun shouldUseTypeTable(): Boolean = true
}

private fun Iterable<PackageParts>.addCompiledPartsAndSort(state: GenerationState): List<PackageParts> =
    addCompiledParts(state).sortedBy { it.packageFqName }

private fun Iterable<PackageParts>.addCompiledParts(state: GenerationState): List<PackageParts> {
    val mapping = state.loadCompiledModule() ?: return this.toList()

    state.incrementalCacheForThisTarget?.getObsoletePackageParts()?.forEach { internalName ->
        val qualifier = JvmClassName.byInternalName(internalName).packageFqName.asString()
        mapping.findPackageParts(qualifier)?.removePart(internalName)
    }

    return (this + mapping.packageFqName2Parts.values)
        .groupBy { it.packageFqName }
        .map { (packageFqName, allOldPackageParts) ->
            PackageParts(packageFqName).apply {
                allOldPackageParts.forEach { packageParts -> this += packageParts }
            }
        }
}

private fun GenerationState.loadCompiledModule(): ModuleMapping? {
    val moduleMappingData = incrementalCacheForThisTarget?.getModuleMappingData() ?: return null
    return ModuleMapping.loadModuleMapping(moduleMappingData, "<incremental>", deserializationConfiguration) { version ->
        throw IllegalStateException("Version of the generated module cannot be incompatible: $version")
    }
}
