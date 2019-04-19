package org.jetbrains.konan.resolve.symbols

import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTableSerializer
import com.jetbrains.cidr.lang.symbols.symtable.SerializerProvider

class KotlinSerializer : SerializerProvider {
    override fun registerSerializers(serializer: FileSymbolTableSerializer) {
        val kryo = serializer.kryo
    }

    override fun getVersion(): Int = 0
}