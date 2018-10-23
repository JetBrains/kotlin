/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan.index

import com.intellij.util.indexing.FileBasedIndex
import org.jetbrains.kotlin.ide.konan.decompiler.KotlinNativeLoadingMetadataCache
import org.jetbrains.kotlin.ide.konan.decompiler.KotlinNativeMetaFileType
import org.jetbrains.kotlin.idea.vfilefinder.KotlinFileIndexBase
import org.jetbrains.kotlin.name.FqName

object KotlinNativeMetaFileIndex : KotlinFileIndexBase<KotlinNativeMetaFileIndex>(KotlinNativeMetaFileIndex::class.java) {

    override fun getIndexer() = INDEXER

    override fun getInputFilter() = FileBasedIndex.InputFilter { it.fileType === KotlinNativeMetaFileType }

    override fun getVersion() = VERSION

    // This is to express intention to index all Kotlin/Native metadata files irrespectively to file size:
    override fun getFileTypesWithSizeLimitNotApplicable() = listOf(KotlinNativeMetaFileType)

    private const val VERSION = 4

    /*todo: check version?!*/
    private val INDEXER = indexer { fileContent ->
        val fragment = KotlinNativeLoadingMetadataCache.getInstance().getCachedPackageFragment(fileContent.file)
        FqName(fragment.fqName)
    }
}
