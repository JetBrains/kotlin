package org.jetbrains.kotlin.analysis.api.dumdum.index

import com.intellij.util.io.DataExternalizer
import java.io.DataInput
import java.io.DataOutput

data class Box<T>(val value: T?) {
    companion object {
        fun <T> externalizer(externalizer: DataExternalizer<T>): DataExternalizer<Box<T>> =
            object : DataExternalizer<Box<T>> {
                override fun save(out: DataOutput, value: Box<T>) {
                    out.writeBoolean(value.value != null)
                    value.value?.let { x ->
                        externalizer.save(out, x)
                    }
                }

                override fun read(`in`: DataInput): Box<T> {
                    val some = `in`.readBoolean()
                    return Box(if (some) externalizer.read(`in`) else null)
                }
            }
    }
}