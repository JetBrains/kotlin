/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model
import org.jetbrains.kotlin.incremental.IncrementalModuleInfo
import org.jetbrains.kotlin.server.IncrementalModuleEntrySetProto
import org.jetbrains.kotlin.server.IncrementalModuleInfoProto
import java.io.File

fun IncrementalModuleInfo.toProto(): IncrementalModuleInfoProto {
    return IncrementalModuleInfoProto.newBuilder()
        .setRootProjectBuildDir(rootProjectBuildDir.absolutePath)
        .putAllDirToModule(
            dirToModule.mapKeys { it.key.absolutePath }
                .mapValues { it.value.toProto() }
        )
        .putAllNameToModules(
            nameToModules.mapValues { (_, moduleSet) ->
                IncrementalModuleEntrySetProto.newBuilder()
                    .addAllEntries(moduleSet.map { it.toProto() })
                    .build()
            }
        )
        .putAllJarToClassListFile(
            jarToClassListFile.mapKeys { it.key.absolutePath }
                .mapValues { it.value.absolutePath }
        )
        .putAllJarToModule(
            jarToModule.mapKeys { it.key.absolutePath }
                .mapValues { it.value.toProto() }
        )
        .putAllJarToAbiSnapshot(
            jarToAbiSnapshot.mapKeys { it.key.absolutePath }
                .mapValues { it.value.absolutePath }
        )
        .build()
}

fun IncrementalModuleInfoProto.toDomain(): IncrementalModuleInfo {
    return IncrementalModuleInfo(
        rootProjectBuildDir = File(rootProjectBuildDir),
        dirToModule = dirToModuleMap.mapKeys { File(it.key) }
            .mapValues { it.value.toDomain() },
        nameToModules = nameToModulesMap.mapValues { (_, moduleSetProto) ->
            moduleSetProto.entriesList.map { it.toDomain() }.toSet()
        },
        jarToClassListFile = jarToClassListFileMap.mapKeys { File(it.key) }
            .mapValues { File(it.value) },
        jarToModule = jarToModuleMap.mapKeys { File(it.key) }
            .mapValues { it.value.toDomain() },
        jarToAbiSnapshot = jarToAbiSnapshotMap.mapKeys { File(it.key) }
            .mapValues { File(it.value) }
    )
}