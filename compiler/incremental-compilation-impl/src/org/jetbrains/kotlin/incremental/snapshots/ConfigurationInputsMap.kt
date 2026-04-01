/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.snapshots

import org.jetbrains.kotlin.incremental.HashedConfigurationInputs
import org.jetbrains.kotlin.incremental.IncrementalCompilationContext
import org.jetbrains.kotlin.incremental.RebuildReason
import org.jetbrains.kotlin.incremental.storage.BasicStringMap
import org.jetbrains.kotlin.incremental.storage.ByteArrayExternalizer
import java.io.File

/**
 * Persists a hash of configuration inputs used in the last successful build.
 * Used to detect when compiler arguments or other configuration affecting compilation
 * outcome change between builds, forcing full recompilation.
 */
internal class ConfigurationInputsMap(
    storageFile: File,
    icContext: IncrementalCompilationContext,
) : BasicStringMap<ByteArray>(storageFile, ByteArrayExternalizer, icContext) {

    sealed interface ConfigurationState {
        data class RequiresRebuild(val reason: RebuildReason) : ConfigurationState
        object UpToDate : ConfigurationState
    }

    @Synchronized
    fun checkConfigurationState(
        hashedConfigurationInputs: HashedConfigurationInputs
    ): ConfigurationState {
        if (!storageFile.exists()) return ConfigurationState.RequiresRebuild(RebuildReason.UNKNOWN_CHANGES_IN_GRADLE_INPUTS)
        if (hashedConfigurationInputs.inputs.size != storage.keys.size) return ConfigurationState.RequiresRebuild(RebuildReason.UNKNOWN_CHANGES_IN_GRADLE_INPUTS)
        for ((rebuildReason, hash) in hashedConfigurationInputs.inputs) {
            val oldValue = storage[rebuildReason.name] ?: return ConfigurationState.RequiresRebuild(RebuildReason.UNKNOWN_CHANGES_IN_GRADLE_INPUTS)
            if (!oldValue.contentEquals(hash)) return ConfigurationState.RequiresRebuild(rebuildReason)
        }
        return ConfigurationState.UpToDate
    }

    @Synchronized
    fun updateHash(hashedConfigurationInputs: HashedConfigurationInputs) {
        val removedKeys = storage.keys.filter { !hashedConfigurationInputs.inputs.containsKey(RebuildReason.valueOf(it)) }
        removedKeys.forEach { storage.remove(it) }
        for ((inputType, newHash) in hashedConfigurationInputs.inputs) {
            storage[inputType.name] = newHash
        }
    }
}
