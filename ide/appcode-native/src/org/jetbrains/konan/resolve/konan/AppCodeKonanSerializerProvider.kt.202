package org.jetbrains.konan.resolve.konan

import com.jetbrains.cidr.lang.symbols.symtable.serialization.FileSymbolTableSerializer
import com.jetbrains.cidr.lang.symbols.symtable.serialization.SerializerProvider

class AppCodeKonanSerializerProvider : SerializerProvider {
    override fun registerSerializers(serializer: FileSymbolTableSerializer) {
        serializer.registerSingletonSerializer { emptyMap<Any, Any>() }
        serializer.registerSingletonSerializer { emptyList<Any>() }
        serializer.registerSingletonSerializer { emptySet<Any>() }
    }

    override fun getVersion(): Int = 1
}