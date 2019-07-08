package org.jetbrains.konan.resolve.symbols.serialization

import com.esotericsoftware.kryo.serializers.DefaultSerializers
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTableSerializer
import com.jetbrains.cidr.lang.symbols.symtable.SerializerProvider
import org.jetbrains.konan.resolve.symbols.*
import org.objenesis.instantiator.ObjectInstantiator

class KtSerializer : SerializerProvider {
    override fun registerSerializers(serializer: FileSymbolTableSerializer) {
        serializer.registerFileOwnerSerializer(KtOCParameterSymbol::class.java, ::KtOCParameterSymbol)
        serializer.registerFileOwnerSerializer(KtOCMethodSymbol::class.java, ::KtOCMethodSymbol)
        serializer.registerFileOwnerSerializer(KtOCPropertySymbol::class.java, ::KtOCPropertySymbol)

        serializer.registerLazySymbolSerializer(KtOCInterfaceSymbol::class.java, ::KtOCInterfaceSymbol)
        serializer.registerLazySymbolSerializer(KtOCProtocolSymbol::class.java, ::KtOCProtocolSymbol)

        serializer.registerFieldSerializer(KtOCInterfaceSymbol.InterfaceState::class.java) { KtOCInterfaceSymbol.InterfaceState() }
        serializer.registerFieldSerializer(KtOCProtocolSymbol.ProtocolState::class.java) { KtOCProtocolSymbol.ProtocolState() }

        serializer.kryo.register(listOf<Any>().javaClass, DefaultSerializers.CollectionsEmptyListSerializer())
    }

    private inline fun <T : KtOCLazySymbol<*, *>> FileSymbolTableSerializer.registerLazySymbolSerializer(
        clazz: Class<T>,
        crossinline initializer: () -> T
    ) {
        kryo.register(clazz, KtLazySymbolSerializer(clazz, this)).apply {
            instantiator = ObjectInstantiator { initializer() }
        }
    }

    override fun getVersion(): Int = 0

    companion object {
        @JvmStatic
        val LOG: Logger = Logger.getInstance(KtSerializer::class.java)
    }
}