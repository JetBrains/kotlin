/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.cri

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain.Companion.FILE_IDS_TO_PATHS_FILENAME
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain.Companion.LOOKUPS_FILENAME
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain.Companion.SUBTYPES_FILENAME
import org.jetbrains.kotlin.incremental.ClassProtoData
import org.jetbrains.kotlin.incremental.KotlinClassInfo
import org.jetbrains.kotlin.incremental.LocalFileKotlinClass
import org.jetbrains.kotlin.incremental.LookupTrackerImpl
import org.jetbrains.kotlin.incremental.components.ICFileMappingTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.storage.BasicFileToPathConverter
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.metadata.deserialization.supertypes
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createDirectories

/**
 * A collector of the compiler reference index for a compilation without incremental caches.
 *
 * Lookups come from [lookupTracker], and subtypes from the Kotlin metadata of the class files reported to
 * [fileMappingTracker]. The compilation must register both trackers in place of its own; they pass everything on to
 * the given delegates.
 */
internal class CompilerReferenceIndexCollector(
    private val directory: Path,
    lookupTrackerDelegate: LookupTracker,
    fileMappingTrackerDelegate: ICFileMappingTracker,
) {
    val lookupTracker = LookupTrackerImpl(lookupTrackerDelegate)

    private val outputFiles = mutableListOf<File>()

    val fileMappingTracker: ICFileMappingTracker = object : ICFileMappingTracker by fileMappingTrackerDelegate {
        override fun recordSourceFilesToOutputFileMapping(sourceFiles: Collection<File>, outputFile: File) {
            outputFiles += outputFile
            fileMappingTrackerDelegate.recordSourceFilesToOutputFileMapping(sourceFiles, outputFile)
        }
    }

    /**
     * Appends the collected data to [directory], reading the class files with [metadataVersion]. It must be the version
     * the compilation wrote them with; otherwise, classes with newer metadata are silently left out of the subtypes.
     */
    @OptIn(ExperimentalBuildToolsApi::class)
    fun write(metadataVersion: MetadataVersion) {
        directory.createDirectories()
        val serializer = CriDataSerializerImpl()
        val lookupData = serializer.serializeLookups(lookupTracker.lookups.toHashMap(), BasicFileToPathConverter)
        val subtypes = collectSubtypes(metadataVersion)
        directory.resolve(LOOKUPS_FILENAME).toFile().appendBytes(lookupData.lookups)
        directory.resolve(FILE_IDS_TO_PATHS_FILENAME).toFile().appendBytes(lookupData.fileIdsToPaths)
        directory.resolve(SUBTYPES_FILENAME).toFile().appendBytes(serializer.serializeSubtypes(subtypes))
    }

    private fun collectSubtypes(metadataVersion: MetadataVersion): Map<FqName, Set<FqName>> {
        val subtypes = mutableMapOf<FqName, MutableSet<FqName>>()
        for (outputFile in outputFiles) {
            if (outputFile.extension != "class") continue
            val kotlinClass = LocalFileKotlinClass.create(outputFile, metadataVersion) ?: continue
            val classProtoData = KotlinClassInfo.readProtoData(kotlinClass.classId, kotlinClass.classHeader) as? ClassProtoData ?: continue
            val proto = classProtoData.proto
            val nameResolver = classProtoData.nameResolver
            val child = nameResolver.getClassId(proto.fqName).asSingleFqName()
            for (supertype in proto.supertypes(TypeTable(proto.typeTable))) {
                val parent = nameResolver.getClassId(supertype.className).asSingleFqName()
                if (parent.asString() != "kotlin.Any") {
                    subtypes.getOrPut(parent) { mutableSetOf() } += child
                }
            }
        }
        return subtypes
    }
}
