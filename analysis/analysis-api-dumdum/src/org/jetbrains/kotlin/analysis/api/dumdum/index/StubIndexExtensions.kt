package org.jetbrains.kotlin.analysis.api.dumdum.index

import com.intellij.util.io.KeyDescriptor

data class StubIndexExtensions(
    val keyTypesMap: KeyTypesMap,
    val indexedSerializedStubTreeType: ValueType<IndexedSerializedStubTree>,
    val extensions: List<StubIndexExtension<*, *>>,
)

fun stubIndexExtensions(
    stubIndexExtensions: List<StubIndexExtension<*, *>>,
): StubIndexExtensions {
    val keyTypesMap = keyTypesMap(
        stubIndexExtensions.map { extension ->
            @Suppress("UNCHECKED_CAST")
            extension.key to extension.keyDescriptor as KeyDescriptor<Any?>
        }
    )
    return StubIndexExtensions(
        keyTypesMap = keyTypesMap,
        indexedSerializedStubTreeType = ValueType(
            id = "stub",
            keys = keyTypesMap.keyTypes.toSet(),
            serializer = IndexedSerializedStubTree.serializer(
                keyTypesMap = keyTypesMap,
            ),
            valueIndexer = ValueIndexer { tree ->
                ValueIndex(
                    tree.index.map { (indexId, map) ->
                        keyTypesMap.keyType(indexId) to map.keys
                    }.toMap()
                )
            }
        ),
        extensions = stubIndexExtensions,
    )
}
