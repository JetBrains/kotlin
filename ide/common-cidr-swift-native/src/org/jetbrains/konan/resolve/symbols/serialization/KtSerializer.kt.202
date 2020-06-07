@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.jetbrains.konan.resolve.symbols.serialization

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.cidr.lang.symbols.symtable.serialization.FileSymbolTableSerializer
import com.jetbrains.cidr.lang.symbols.symtable.serialization.SerializerProvider
import com.jetbrains.swift.symbols.impl.registerProjectAndFileOwnerSerializer
import org.jetbrains.konan.resolve.symbols.KtDependencyMarker
import org.jetbrains.konan.resolve.symbols.KtLazySymbol
import org.jetbrains.konan.resolve.symbols.objc.*
import org.jetbrains.konan.resolve.symbols.swift.*
import org.objenesis.instantiator.ObjectInstantiator

class KtSerializer : SerializerProvider {
    override fun registerSerializers(serializer: FileSymbolTableSerializer) {
        serializer.registerFileOwnerSerializer(KtOCParameterSymbol::class.java, ::KtOCParameterSymbol)
        serializer.registerFileOwnerSerializer(KtOCMethodSymbol::class.java, ::KtOCMethodSymbol)
        serializer.registerFileOwnerSerializer(KtOCPropertySymbol::class.java, ::KtOCPropertySymbol)

        serializer.registerOCLazySymbolSerializer(KtOCInterfaceSymbol::class.java, ::KtOCInterfaceSymbol)
        serializer.registerOCLazySymbolSerializer(KtOCProtocolSymbol::class.java, ::KtOCProtocolSymbol)

        serializer.registerFieldSerializer(KtOCInterfaceSymbol.InterfaceState::class.java, KtOCInterfaceSymbol::InterfaceState)
        serializer.registerFieldSerializer(KtOCProtocolSymbol.ProtocolState::class.java, KtOCProtocolSymbol::ProtocolState)

        serializer.registerProjectAndFileOwnerSerializer(::KtSwiftParameterSymbol)
        serializer.registerProjectAndFileOwnerSerializer(::KtSwiftMethodSymbol)
        serializer.registerProjectAndFileOwnerSerializer(::KtSwiftInitializerSymbol)
        serializer.registerProjectAndFileOwnerSerializer(::KtSwiftPropertySymbol)

        serializer.registerSwiftLazySymbolSerializer(KtSwiftClassSymbol::class.java, ::KtSwiftClassSymbol)
        serializer.registerSwiftLazySymbolSerializer(KtSwiftProtocolSymbol::class.java, ::KtSwiftProtocolSymbol)
        serializer.registerSwiftLazySymbolSerializer(KtSwiftExtensionSymbol::class.java, ::KtSwiftExtensionSymbol)

        serializer.registerFieldSerializer(KtLazySymbol.AbortedState::class.java) { KtLazySymbol.AbortedState() }
        serializer.registerFieldSerializer(KtSwiftClassSymbol.ClassState::class.java, KtSwiftClassSymbol::ClassState)
        serializer.registerFieldSerializer(KtSwiftProtocolSymbol.ProtocolState::class.java, KtSwiftProtocolSymbol::ProtocolState)
        serializer.registerFieldSerializer(KtSwiftExtensionSymbol.ExtensionState::class.java, KtSwiftExtensionSymbol::ExtensionState)

        serializer.kryo.register(KtDependencyMarker::class.java, KtDependencyMarkerSerializer(serializer))
    }

    private inline fun <T : KtOCLazySymbol<*, *>> FileSymbolTableSerializer.registerOCLazySymbolSerializer(
        clazz: Class<T>,
        crossinline initializer: () -> T
    ) {
        kryo.register(clazz, KtOCLazySymbolSerializer(clazz, this)).apply {
            instantiator = ObjectInstantiator { initializer() }
        }
    }

    private inline fun <T : KtSwiftLazySymbol<*, *>> FileSymbolTableSerializer.registerSwiftLazySymbolSerializer(
        clazz: Class<T>,
        crossinline initializer: () -> T
    ) {
        kryo.register(clazz, KtSwiftLazySymbolSerializer(clazz, this)).apply {
            instantiator = ObjectInstantiator { initializer() }
        }
    }

    override fun getVersion(): Int = 8

    companion object {
        @JvmStatic
        val LOG: Logger = Logger.getInstance(KtSerializer::class.java)
    }
}