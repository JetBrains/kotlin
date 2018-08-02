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

    override fun getInputFilter() = FileBasedIndex.InputFilter { it.fileType === KonanMetaFileType }

    override fun getIndexer() = dataIndexer

    override fun getVersion() = VERSION
}
