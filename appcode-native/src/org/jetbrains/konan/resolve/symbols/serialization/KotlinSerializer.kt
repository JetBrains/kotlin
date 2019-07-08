package org.jetbrains.konan.resolve.symbols.serialization

import com.esotericsoftware.kryo.serializers.DefaultSerializers
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTableSerializer
import com.jetbrains.cidr.lang.symbols.symtable.SerializerProvider
import org.jetbrains.konan.resolve.symbols.*
import org.objenesis.instantiator.ObjectInstantiator

class KotlinSerializer : SerializerProvider {
    override fun registerSerializers(serializer: FileSymbolTableSerializer) {
        serializer.registerFileOwnerSerializer(KotlinOCParameterSymbol::class.java, ::KotlinOCParameterSymbol)
        serializer.registerFileOwnerSerializer(KotlinOCMethodSymbol::class.java, ::KotlinOCMethodSymbol)
        serializer.registerFileOwnerSerializer(KotlinOCPropertySymbol::class.java, ::KotlinOCPropertySymbol)

        serializer.registerLazySymbolSerializer(KotlinOCInterfaceSymbol::class.java, ::KotlinOCInterfaceSymbol)
        serializer.registerLazySymbolSerializer(KotlinOCProtocolSymbol::class.java, ::KotlinOCProtocolSymbol)

        serializer.registerFieldSerializer(KotlinOCInterfaceSymbol.InterfaceState::class.java) { KotlinOCInterfaceSymbol.InterfaceState() }
        serializer.registerFieldSerializer(KotlinOCProtocolSymbol.ProtocolState::class.java) { KotlinOCProtocolSymbol.ProtocolState() }

        serializer.kryo.register(listOf<Any>().javaClass, DefaultSerializers.CollectionsEmptyListSerializer())
    }

    private inline fun <T : KotlinOCWrapperSymbol<*, *>> FileSymbolTableSerializer.registerLazySymbolSerializer(
        clazz: Class<T>,
        crossinline initializer: () -> T
    ) {
        kryo.register(clazz, KtLazySymbolSerializer(clazz, this)).apply {
            instantiator = ObjectInstantiator { initializer() }
        }
    }

    override fun getVersion(): Int = 1

    companion object {
        @JvmStatic
        val LOG: Logger = Logger.getInstance(KotlinSerializer::class.java)
    }
}