package org.jetbrains.kotlin.analysis.api.dumdum

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID

private data class Box<T>(val value: T)

private fun <K, V> ID<K, V>.indexValueDescriptor(): ValueDescriptor<Map<K, Box<V>>> =
    ValueDescriptor("indexValue${name}", Serializer.dummy())

fun interface VirtualFileFactory {
    fun virtualFile(fileId: FileId): VirtualFile
}

fun Index.fileBased(virtualFileFactory: VirtualFileFactory): FileBasedIndex = let { index ->
    object : FileBasedIndex {
        override fun <K, V> processValues(
            indexId: ID<K, V>,
            dataKey: K,
            filter: GlobalSearchScope,
            processor: Processor<in V>,
        ): Boolean {
            val valueDescriptor = indexId.indexValueDescriptor()
            val keyDescriptor = indexId.asKeyDescriptor()
            return index
                .files(IndexKey(keyDescriptor, dataKey))
                .filter { filter.contains(virtualFileFactory.virtualFile(it)) }
                .mapNotNull { documentId ->
                    index
                        .value(documentId, valueDescriptor)
                        ?.get(dataKey)
                }
                .all { (v) -> processor.process(v) }
        }

        override fun <K, V> processAllKeys(
            indexId: ID<K, V>,
            filter: GlobalSearchScope,
            processor: Processor<in K>,
        ): Boolean {
            val keyDescriptor = indexId.asKeyDescriptor()
            return index
                .keys(keyDescriptor)
                .filter { k ->
                    index
                        .files(IndexKey(keyDescriptor, k))
                        .any { filter.contains(virtualFileFactory.virtualFile(it)) }
                }
                .all(processor::process)
        }
    }
}

fun fileBasedIndexesUpdates(fileId: FileId, fileContent: FileContent, extensions: List<FileBasedIndexExtension<*, *>>): List<IndexUpdate<*>> =
    extensions.map { extension ->
        @Suppress("UNCHECKED_CAST")
        extension as FileBasedIndexExtension<Any, Any?>
        val indexId = extension.name
        val map = extension.indexer.map(fileContent).mapValues { (_, v) -> Box(v) }
        val keyDescriptor = indexId.asKeyDescriptor()
        IndexUpdate(
            fileId = fileId,
            valueType = indexId.indexValueDescriptor(),
            value = map,
            keys = map.keys.map { key ->
                IndexKey(keyDescriptor, key)
            }
        )
    }
