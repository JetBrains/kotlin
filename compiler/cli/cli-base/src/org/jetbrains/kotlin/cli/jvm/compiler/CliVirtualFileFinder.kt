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
 * Finds the [VirtualFile]s of classes in [index], applying [filter] during index traversal.
 *
 * Compiler code should use [JvmIndexKotlinClassFinder] instead.
 *
 * TODO(KT-90203): drop this finder once the Analysis API adapters read handles.
 */
class CliVirtualFileFinder @VirtualFileBasedApi constructor(
    index: JvmDependenciesIndex,
    accepts: (VirtualFile) -> Boolean,
    enableSearchInCtSym: Boolean,
    perfManager: PerformanceManager?,
) : VirtualFileFinder(perfManager) {
    @OptIn(VirtualFileBasedApi::class)
    private val classFiles =
        JvmIndexKotlinClassFinder(index, { accepts(it.virtualFile) }, enableSearchInCtSym, perfManager)

    // The two `VirtualFile` lookups stay for the backend (`inlineCodegenUtils.findVirtualFile`) and the IDE.
    @OptIn(VirtualFileBasedApi::class)
    override fun findVirtualFileWithHeader(classId: ClassId): VirtualFile? =
        classFiles.findBinaryClassFile(classId)?.virtualFile

    @OptIn(VirtualFileBasedApi::class)
    override fun findSourceOrBinaryVirtualFile(classId: ClassId): VirtualFile? =
        classFiles.findSourceOrBinaryClassFile(classId)?.virtualFile

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
