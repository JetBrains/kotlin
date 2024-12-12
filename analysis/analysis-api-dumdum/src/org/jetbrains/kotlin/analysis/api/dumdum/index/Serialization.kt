package org.jetbrains.kotlin.analysis.api.dumdum.index

import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.UnsyncByteArrayInputStream
import com.intellij.util.io.UnsyncByteArrayOutputStream
import java.io.DataInput
import java.io.DataInputStream
import java.io.DataOutput
import java.io.DataOutputStream

class MapExternalizer<K, V>(
    val keyExternalizer: DataExternalizer<K>,
    val valueExternalizer: DataExternalizer<V>,
) : DataExternalizer<Map<K, V>> {
    override fun save(out: DataOutput, value: Map<K, V>) {
        out.writeInt(value.size)
        for ((k, v) in value) {
            keyExternalizer.save(out, k)
            valueExternalizer.save(out, v)
        }
    }

    override fun read(`in`: DataInput): Map<K, V> {
        val size = `in`.readInt()
        return buildMap(size) {
            repeat(size) {
                val k = keyExternalizer.read(`in`)
                val v = valueExternalizer.read(`in`)
                put(k, v)
            }
        }
    }
}

fun <T> DataExternalizer<T>.asSerializer(): Serializer<T> = let { externalizer ->
    object : Serializer<T> {
        override fun serialize(value: T): ByteArray {
            val baos = UnsyncByteArrayOutputStream()
            baos.use { os ->
                DataOutputStream(os).use { dos ->
                    externalizer.save(dos, value)
                }
            }
            return baos.toByteArray()
        }

        override fun deserialize(bytes: ByteArray): T =
            UnsyncByteArrayInputStream(bytes).use { i ->
                DataInputStream(i).use { dis ->
                    externalizer.read(dis)
                }
            }
    }
}
