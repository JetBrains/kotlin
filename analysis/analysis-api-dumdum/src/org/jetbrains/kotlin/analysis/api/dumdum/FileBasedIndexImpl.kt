package org.jetbrains.kotlin.analysis.api.dumdum

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID

private data class Box<T>(val value: T)

private fun <K, V> ID<K, V>.indexValueDescriptor(): ValueDescriptor<Map<K, Box<V>>> =
    ValueDescriptor("indexValue${name}", Serializer.dummy())

fun interface FileLocator {
    fun locate(dodcumentId: DocumentId<*>): VirtualFile
}

fun Index.fileBased(fileLocator: FileLocator): FileBasedIndex = let { index ->
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
                .documents(IndexKey(keyDescriptor, dataKey))
                .filter { filter.contains(fileLocator.locate(it)) }
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
                        .documents(IndexKey(keyDescriptor, k))
                        .any { filter.contains(fileLocator.locate(it)) }
                }
                .all(processor::process)
        }
    }
}

fun fileBasedIndexesUpdates(fileContent: FileContent, extensions: List<FileBasedIndexExtension<*, *>>): List<IndexUpdate<*>> =
    extensions.map { extension ->
        @Suppress("UNCHECKED_CAST")
        extension as FileBasedIndexExtension<Any, Any?>
        val indexId = extension.name
        val map = extension.indexer.map(fileContent).mapValues { (_, v) -> Box(v) }
        val keyDescriptor = indexId.asKeyDescriptor()
        IndexUpdate(
            documentId = DocumentId(VirtualFileDocumentIdDescriptor, fileContent.file),
            valueType = indexId.indexValueDescriptor(),
            value = map,
            keys = map.keys.map { key ->
                IndexKey(keyDescriptor, key)
            }
        )
    }
