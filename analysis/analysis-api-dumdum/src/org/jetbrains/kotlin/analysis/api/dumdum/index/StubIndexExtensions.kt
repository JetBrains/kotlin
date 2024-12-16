package org.jetbrains.kotlin.analysis.api.dumdum.index

import com.intellij.util.io.KeyDescriptor

data class StubIndexExtensions(
    val keyTypesMap: KeyTypesMap,
    val indexedSerializedStubTreeType: ValueType<IndexedSerializedStubTree>,
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
            serializer = IndexedSerializedStubTree.serializer(
                keyTypesMap = keyTypesMap,
            )
        ),
    )
}
