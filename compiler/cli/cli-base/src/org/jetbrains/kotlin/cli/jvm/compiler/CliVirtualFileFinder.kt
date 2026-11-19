/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndex
import org.jetbrains.kotlin.cli.jvm.index.JvmIndexKotlinClassFinder
import org.jetbrains.kotlin.cli.jvm.index.virtualFile
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.VirtualFileBasedApi
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.util.PerformanceManager
import java.io.InputStream

/**
 * Finds classes with [classFiles], and the [VirtualFile]s of classes with [virtualFiles], which must read an index with VFS entries.
 *
 * Compiler code should use [JvmIndexKotlinClassFinder] instead.
 *
 * TODO(KT-90203): drop this finder once the Analysis API adapters read handles.
 */
class CliVirtualFileFinder(
    private val classFiles: JvmIndexKotlinClassFinder,
    private val virtualFiles: JvmIndexKotlinClassFinder,
    perfManager: PerformanceManager?,
) : VirtualFileFinder(perfManager) {
    /** Finds the classes of [index] whose [VirtualFile] [accepts]; [index] must have VFS entries. */
    @VirtualFileBasedApi
    constructor(
        index: JvmDependenciesIndex,
        accepts: (VirtualFile) -> Boolean,
        enableSearchInCtSym: Boolean,
        perfManager: PerformanceManager?,
    ) : this(JvmIndexKotlinClassFinder(index, { accepts(it.virtualFile) }, enableSearchInCtSym, perfManager), perfManager)

    private constructor(classFiles: JvmIndexKotlinClassFinder, perfManager: PerformanceManager?) :
            this(classFiles, classFiles, perfManager)

    // The two `VirtualFile` lookups stay for the Analysis API and the IDE.
    override fun findVirtualFileWithHeader(classId: ClassId): VirtualFile? =
        @OptIn(VirtualFileBasedApi::class) virtualFiles.findBinaryClassFile(classId)?.virtualFile

    override fun findBinaryClassBytes(classId: ClassId): ByteArray? =
        classFiles.findBinaryClassFile(classId)?.readBytes()

    override fun findSourceOrBinaryVirtualFile(classId: ClassId): VirtualFile? =
        @OptIn(VirtualFileBasedApi::class) virtualFiles.findSourceOrBinaryClassFile(classId)?.virtualFile

    override fun findMetadata(classId: ClassId): InputStream? =
        classFiles.findMetadata(classId)

    /** Not restricted by the file filter: this lookup covers all indexed roots. */
    override fun findMetadataTopLevelClassesInPackage(packageFqName: FqName): Set<String> =
        classFiles.findMetadataTopLevelClassesInPackage(packageFqName)

    /** Not restricted by the file filter: this lookup covers all indexed roots. */
    override fun hasMetadataPackage(fqName: FqName): Boolean =
        classFiles.hasMetadataPackage(fqName)

    /** Not restricted by the file filter: this lookup covers all indexed roots. */
    override fun findBuiltInsData(packageFqName: FqName): InputStream? =
        classFiles.findBuiltInsData(packageFqName)
}
