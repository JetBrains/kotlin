package org.jetbrains.konan.resolve.symbols.serialization

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.intellij.testFramework.LightVirtualFile
import com.jetbrains.cidr.lang.symbols.symtable.serialization.FileSymbolTableSerializer
import com.jetbrains.cidr.lang.symbols.symtable.serialization.OCIncludeSerializer
import org.jetbrains.konan.resolve.symbols.KtDependencyMap
import org.jetbrains.konan.resolve.symbols.KtDependencyMarker

internal class KtDependencyMarkerSerializer(private val serializer: FileSymbolTableSerializer) : Serializer<KtDependencyMarker>() {
    override fun write(kryo: Kryo, output: Output, marker: KtDependencyMarker) {
        val dependencies = marker.dependencies
        output.writeInt(dependencies.size(), true)
        val iterator = dependencies.iterator()
        while (iterator.hasNext()) {
            iterator.advance()
            OCIncludeSerializer.writeTargetFile(output, iterator.key())
            output.writeLong(iterator.value())
        }
    }

    override fun read(kryo: Kryo, input: Input, type: Class<KtDependencyMarker>): KtDependencyMarker {
        val size = input.readInt(true)
        val dependencies = KtDependencyMap(size)
        repeat(size) {
            dependencies.put(OCIncludeSerializer.readTargetFile(input, serializer) ?: InvalidFile, input.readLong())
        }
        return KtDependencyMarker(serializer.currentFile, dependencies, true)
    }

    private object InvalidFile : LightVirtualFile() {
        override fun isValid(): Boolean = false
    }
}