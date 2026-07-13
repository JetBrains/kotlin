/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.backend.common.output.OutputFile
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.load.kotlin.loadModuleMapping
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping
import org.jetbrains.kotlin.metadata.jvm.deserialization.PackageParts
import org.jetbrains.kotlin.resolve.CommonCompilerDeserializationConfiguration
import org.jetbrains.kotlin.resolve.jvm.JvmClassName

fun ClassFileFactory.getClassFiles(): Iterable<OutputFile> {
    return asList().filterClassFiles()
}

fun List<OutputFile>.filterClassFiles(): List<OutputFile> {
    return filter { it.relativePath.endsWith(".class") }
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
        .map { [packageFqName, allOldPackageParts] ->
            PackageParts(packageFqName).apply {
                allOldPackageParts.forEach { packageParts -> this += packageParts }
            }
        }
}

fun GenerationState.loadCompiledModule(): ModuleMapping? {
    val moduleMappingData = incrementalCacheForThisTarget?.getModuleMappingData() ?: return null
    val deserializationConfiguration = CommonCompilerDeserializationConfiguration(config.languageVersionSettings)
    return ModuleMapping.loadModuleMapping(moduleMappingData, "<incremental>", deserializationConfiguration) { version ->
        throw IllegalStateException("Version of the generated module cannot be incompatible: $version")
    }
}
