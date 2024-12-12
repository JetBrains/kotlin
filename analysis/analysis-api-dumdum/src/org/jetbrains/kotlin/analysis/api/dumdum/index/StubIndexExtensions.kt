package org.jetbrains.kotlin.analysis.api.dumdum.index

import com.intellij.util.io.KeyDescriptor

data class StubIndexExtensions(
    val keyTypesMap: KeyTypesMap,
    val stubValueType: ValueType<StubValue>,
)

fun stubIndexExtensions(
    stubIndexExtensions: List<StubIndexExtension<*, *>>,
    stubSerializersTable: StubSerializersTable,
): StubIndexExtensions {
    val keyTypesMap = keyTypesMap(
        stubIndexExtensions.map { extension ->
            @Suppress("UNCHECKED_CAST")
            extension.key to extension.keyDescriptor as KeyDescriptor<Any?>
        }
    )
    return StubIndexExtensions(
        keyTypesMap = keyTypesMap,
        stubValueType = ValueType(
            id = "stub",
            serializer = StubValue.serializer(
                keyTypesMap = keyTypesMap,
                stubSerializersTable = stubSerializersTable
            )
        ),
    )
}
