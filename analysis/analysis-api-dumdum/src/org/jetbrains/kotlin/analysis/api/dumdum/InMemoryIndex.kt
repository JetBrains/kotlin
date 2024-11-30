package org.jetbrains.kotlin.analysis.api.dumdum

import org.jetbrains.kotlin.utils.addToStdlib.flatGroupBy

fun inMemoryIndex(updates: List<IndexUpdate<*>>): Index {
    val byId = updates.groupBy { it.documentId to it.valueType }
    val byKeys = updates.flatGroupBy { it.keys }
    val byKeyDescriptor = updates.flatGroupBy { it.keys.map(IndexKey<*>::keyDescriptor) }
    return object : Index {
        @Suppress("UNCHECKED_CAST")
        override fun <S> value(documentId: DocumentId<*>, valueDescriptor: ValueDescriptor<S>): S? =
            byId[documentId to valueDescriptor]?.firstOrNull()?.value as S?

        override fun <K> documents(key: IndexKey<K>): Sequence<DocumentId<*>> =
            byKeys[key]?.asSequence()?.map { it.documentId }.orEmpty()

        override fun <K> keys(keyDescriptor: KeyDescriptor<K>): Sequence<K> =
            byKeyDescriptor[keyDescriptor]?.asSequence()?.flatMap { it.keys }?.map {
                @Suppress("UNCHECKED_CAST")
                it.key as K
            }.orEmpty()
    }
}