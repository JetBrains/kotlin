package org.jetbrains.kotlin.analysis.api.dumdum.index

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.indexing.ID

fun Index.fileBased(
    virtualFileFactory: VirtualFileFactory,
    fileBasedIndexExtensions: FileBasedIndexExtensions,
): FileBasedIndex = let { index ->
    object : FileBasedIndex {
        override fun <K, V> processValues(
            indexId: ID<K, V>,
            dataKey: K,
            filter: GlobalSearchScope,
            processor: Processor<in V>,
        ): Boolean {
            val valueDescriptor = fileBasedIndexExtensions.mapType(indexId)
            val keyType = fileBasedIndexExtensions.keyTypesMap.keyType(indexId)
            return index
                .files(IndexKey(keyType, dataKey))
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
            val keyType = fileBasedIndexExtensions.keyTypesMap.keyType(indexId)
            return index
                .keys(keyType)
                .filter { k ->
                    index
                        .files(IndexKey(keyType, k))
                        .any { filter.contains(virtualFileFactory.virtualFile(it)) }
                }
                .all(processor::process)
        }
    }
}
