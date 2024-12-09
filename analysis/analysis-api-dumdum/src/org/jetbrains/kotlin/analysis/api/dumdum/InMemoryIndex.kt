package org.jetbrains.kotlin.analysis.api.dumdum

import org.jetbrains.kotlin.utils.addToStdlib.flatGroupBy

fun inMemoryIndex(updates: List<IndexUpdate<*>>): Index {
    val byFile = updates.groupBy { it.fileId to it.valueType }
    val byKey = updates.flatGroupBy { it.keys }
    val byKeyDescriptor = updates.flatGroupBy { it.keys.map(IndexKey<*>::keyDescriptor) }
    return object : Index {
        @Suppress("UNCHECKED_CAST")
        override fun <S> value(fileId: FileId, valueDescriptor: ValueDescriptor<S>): S? =
            byFile[fileId to valueDescriptor]?.firstOrNull()?.value as S?

        override fun <K> files(key: IndexKey<K>): Sequence<FileId> =
            byKey[key]?.asSequence()?.map { it.fileId }.orEmpty()

        override fun <K> keys(keyDescriptor: KeyDescriptor<K>): Sequence<K> =
            byKeyDescriptor[keyDescriptor]?.asSequence()?.flatMap { it.keys }?.map {
                @Suppress("UNCHECKED_CAST")
                it.key as K
            }.orEmpty()
    }
}