package org.jetbrains.konan.resolve.symbols.serialization

import com.esotericsoftware.kryo.serializers.DefaultSerializers
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTableSerializer
import com.jetbrains.cidr.lang.symbols.symtable.SerializerProvider
import org.jetbrains.konan.resolve.symbols.objc.*
import org.jetbrains.konan.resolve.symbols.swift.*
import org.objenesis.instantiator.ObjectInstantiator
import java.lang.reflect.Method

class KtSerializer : SerializerProvider {
    override fun registerSerializers(serializer: FileSymbolTableSerializer) {
        serializer.registerFileOwnerSerializer(KtOCParameterSymbol::class.java, ::KtOCParameterSymbol)
        serializer.registerFileOwnerSerializer(KtOCMethodSymbol::class.java, ::KtOCMethodSymbol)
        serializer.registerFileOwnerSerializer(KtOCPropertySymbol::class.java, ::KtOCPropertySymbol)

        serializer.registerOCLazySymbolSerializer(KtOCInterfaceSymbol::class.java, ::KtOCInterfaceSymbol)
        serializer.registerOCLazySymbolSerializer(KtOCProtocolSymbol::class.java, ::KtOCProtocolSymbol)

        serializer.registerFieldSerializer(KtOCInterfaceSymbol.InterfaceState::class.java) { KtOCInterfaceSymbol.InterfaceState() }
        serializer.registerFieldSerializer(KtOCProtocolSymbol.ProtocolState::class.java) { KtOCProtocolSymbol.ProtocolState() }

        serializer.registerProjectAndFileOwnerSerializer(KtSwiftParameterSymbol::class.java, ::KtSwiftParameterSymbol)
        serializer.registerProjectAndFileOwnerSerializer(KtSwiftMethodSymbol::class.java, ::KtSwiftMethodSymbol)
        serializer.registerProjectAndFileOwnerSerializer(KtSwiftPropertySymbol::class.java, ::KtSwiftPropertySymbol)

        serializer.registerSwiftLazySymbolSerializer(KtSwiftClassSymbol::class.java, ::KtSwiftClassSymbol)
        serializer.registerSwiftLazySymbolSerializer(KtSwiftProtocolSymbol::class.java, ::KtSwiftProtocolSymbol)
        serializer.registerSwiftLazySymbolSerializer(KtSwiftExtensionSymbol::class.java, ::KtSwiftExtensionSymbol)

//        serializer.registerFieldSerializer(KtOCInterfaceSymbol.InterfaceState::class.java) { KtOCInterfaceSymbol.InterfaceState() }
//        serializer.registerFieldSerializer(KtOCProtocolSymbol.ProtocolState::class.java) { KtOCProtocolSymbol.ProtocolState() }

        serializer.kryo.register(listOf<Any>().javaClass, DefaultSerializers.CollectionsEmptyListSerializer())
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


    override fun getVersion(): Int = 0

    companion object {
        @JvmStatic
        val LOG: Logger = Logger.getInstance(KtSerializer::class.java)

        internal val FileSymbolTableSerializer.currentFile: VirtualFile
            get() = CurrentFileGetter.get(this)

        internal val FileSymbolTableSerializer.project: Project
            get() = ProjectGetter.get(this)

        //todo get rid of reflection
        internal object CurrentFileGetter {
            private val method: Method = FileSymbolTableSerializer::class.java.getMethod("getCurrentFile")
            fun get(serializer: FileSymbolTableSerializer): VirtualFile = method.invoke(serializer) as VirtualFile
        }

        //todo get rid of reflection
        internal object ProjectGetter {
            private val method: Method = FileSymbolTableSerializer::class.java.getMethod("getProject")
            fun get(serializer: FileSymbolTableSerializer): Project = method.invoke(serializer) as Project
        }

    }
}