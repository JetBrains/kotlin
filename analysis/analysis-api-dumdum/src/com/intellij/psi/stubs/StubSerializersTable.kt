package com.intellij.psi.stubs

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.StubFileElementType

data class StubSerializersTable(val map: Map<String, ObjectStubSerializer<*, *>>) {
    fun getSerializer(serializerName: String): ObjectStubSerializer<*, *> =
        map[serializerName]!!

    companion object {
        fun build(): StubSerializersTable =
            StubSerializersTable(buildMap {
                object {
                    fun registerSerializer(serializer: ObjectStubSerializer<*, *>) {
                        put(serializer.externalId, serializer)
                    }
                }.run {
                    registerSerializer(PsiFileStubImpl.TYPE);
                    val lazySerializers = IStubElementType.loadRegisteredStubElementTypes()
                    val stubElementTypes = IElementType.enumerate { it is StubSerializer<*> }
                    for (type in stubElementTypes) {
                        if (type is StubFileElementType<*> &&
                            StubFileElementType.DEFAULT_EXTERNAL_ID == type.externalId
                        ) {
                            continue;
                        }
                        registerSerializer(type as StubSerializer<*>)
                    }

                    for (lazySerializer in lazySerializers) {
                        registerSerializer(lazySerializer.get());
                    }
                }
            })
    }
}