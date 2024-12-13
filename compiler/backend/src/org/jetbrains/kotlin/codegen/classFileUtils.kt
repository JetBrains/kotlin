/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.backend.common.output.OutputFile
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.load.kotlin.loadModuleMapping
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping
import org.jetbrains.kotlin.metadata.jvm.deserialization.PackageParts
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.KotlinSerializerExtensionBase
import org.jetbrains.kotlin.serialization.StringTableImpl
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol

fun ClassFileFactory.getClassFiles(): Iterable<OutputFile> {
    return asList().filterClassFiles()
}

fun ClassFileFactory.getKotlinModuleFile(): OutputFile? = asList().filter { it.relativePath.endsWith(".kotlin_module") }.run {
    when (size) {
        0 -> null
        1 -> single()
        else -> error("Module has non-unique .kotlin_metadata file")
    }
}

fun List<OutputFile>.filterClassFiles(): List<OutputFile> {
    return filter { it.relativePath.endsWith(".class") }
}

class JvmOptionalAnnotationSerializerExtension(
    override val stringTable: StringTableImpl
) : KotlinSerializerExtensionBase(BuiltInSerializerProtocol) {
    override val metadataVersion: BinaryVersion
        get() = MetadataVersion.INSTANCE

    override fun shouldUseTypeTable(): Boolean = true
}

fun Iterable<PackageParts>.addCompiledPartsAndSort(state: GenerationState): List<PackageParts> =
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

fun GenerationState.loadCompiledModule(): ModuleMapping? {
    val moduleMappingData = incrementalCacheForThisTarget?.getModuleMappingData() ?: return null
    val deserializationConfiguration = CompilerDeserializationConfiguration(config.languageVersionSettings)
    return ModuleMapping.loadModuleMapping(moduleMappingData, "<incremental>", deserializationConfiguration) { version ->
        throw IllegalStateException("Version of the generated module cannot be incompatible: $version")
    }
}
