package org.jetbrains.kotlin.analysis.api.dumdum.filebasedindex.names

import com.intellij.util.io.DataExternalizer
import org.jetbrains.kotlin.analysis.api.dumdum.index.FileBasedIndexExtension
import java.io.DataInput
import java.io.DataOutput

abstract class ScalarIndexExtension<T> : FileBasedIndexExtension<T, Unit> {
    override val valueExternalizer: DataExternalizer<Unit>
        get() = object : DataExternalizer<Unit> {
            override fun save(out: DataOutput, value: Unit?) {
            }

            override fun read(`in`: DataInput) {
            }
        }
}