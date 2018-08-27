/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.analyser.index

import com.intellij.util.indexing.FileBasedIndex
import org.jetbrains.kotlin.idea.vfilefinder.KotlinFileIndexBase
import org.jetbrains.kotlin.name.FqName

class KonanMetaFileIndex
    : KotlinFileIndexBase<KonanMetaFileIndex>(KonanMetaFileIndex::class.java) {

    companion object {
        private const val VERSION = 4
    }

    /*todo: check version?!*/
    private val dataIndexer = indexer { fileContent ->
        val fragment = KonanDescriptorManager.getInstance().getCachedPackageFragment(fileContent.file)
        FqName(fragment.fqName)
    }

    // this is to express intention to index all Kotlin/Native metadata files irrespectively to file size
    override fun getFileTypesWithSizeLimitNotApplicable() = listOf(KonanMetaFileType)

    override fun getInputFilter() = FileBasedIndex.InputFilter { it.fileType === KonanMetaFileType }

    override fun getIndexer() = dataIndexer

    override fun getVersion() = VERSION
}
