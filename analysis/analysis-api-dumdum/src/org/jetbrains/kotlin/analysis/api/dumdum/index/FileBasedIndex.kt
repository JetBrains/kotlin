package org.jetbrains.kotlin.analysis.api.dumdum.index

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.KeyDescriptor

interface FileBasedIndex {
    fun <K> getContainingFilesIterator(
        indexId: ID<K, *>,
        key: K,
        scope: GlobalSearchScope,
    ): Iterator<VirtualFile>

    fun <K, V> processValues(
        indexId: ID<K, V>,
        dataKey: K,
        filter: GlobalSearchScope,
        processor: Processor<in V>,
    ): Boolean

    fun <K, V> processAllKeys(
        indexId: ID<K, V>,
        filter: GlobalSearchScope,
        processor: Processor<in K>,
    ): Boolean
}

interface FileBasedIndexExtension<K, V> {
    val name: ID<K, V>
    val version: Int
    val keyDescriptor: KeyDescriptor<K>
    val valueExternalizer: DataExternalizer<V>
    val inputFilter: List<FileType>
    val indexer: DataIndexer<K, V, FileContent>
}

fun <K, V> FileBasedIndex.getValues(name: ID<K, V>, fqName: K, scope: GlobalSearchScope): List<V> =
    buildList {
        processValues(name, fqName, scope) {
            add(it)
            true
        }
    }

