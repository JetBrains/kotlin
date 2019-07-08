package org.jetbrains.konan.resolve.symbols.serialization

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.serializers.FieldSerializer
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTableSerializer
import org.jetbrains.konan.resolve.symbols.KtOCLazySymbol
import java.lang.reflect.Method

class KtLazySymbolSerializer<T : KtOCLazySymbol<*, *>>(
    symbolClass: Class<T>,
    private val serializer: FileSymbolTableSerializer
) : FieldSerializer<T>(serializer.kryo, symbolClass) {

    override fun create(kryo: Kryo, input: Input?, type: Class<T>): T {
        val result = (super.create(kryo, input, type) as T)
        result.init(serializer.currentFile)
        return result
    }

    override fun createCopy(kryo: Kryo, original: T): T {
        val result = super.createCopy(kryo, original) as T
        val file = original.containingFile
        KtSerializer.LOG.assertTrue(file == serializer.currentFile)
        result.init(file)
        return result
    }

    override fun write(kryo: Kryo?, output: Output?, symbol: T) {
        symbol.ensureStateLoaded()
        super.write(kryo, output, symbol)
    }

    private val FileSymbolTableSerializer.currentFile: VirtualFile
        get() = CurrentFileGetter.get(this)
}

//todo get rid of reflection
private object CurrentFileGetter {
    private val method: Method = FileSymbolTableSerializer::class.java.getMethod("getCurrentFile")
    fun get(serializer: FileSymbolTableSerializer): VirtualFile = method.invoke(serializer) as VirtualFile
}



