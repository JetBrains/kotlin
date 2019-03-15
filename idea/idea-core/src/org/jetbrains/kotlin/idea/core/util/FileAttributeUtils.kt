/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.util

import com.intellij.openapi.util.NullableLazyKey
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileWithId
import com.intellij.openapi.vfs.newvfs.FileAttribute
import com.intellij.util.io.DataInputOutputUtil.readSeq
import com.intellij.util.io.DataInputOutputUtil.writeSeq
import com.intellij.util.io.IOUtil.readUTF
import com.intellij.util.io.IOUtil.writeUTF
import java.io.*
import kotlin.reflect.KProperty

inline fun <T : Any> cachedFileAttribute(
    name: String,
    version: Int,
    crossinline read: DataInputStream.() -> T,
    crossinline write: DataOutputStream.(T) -> Unit
): FileAttributeProperty<T> {
    return object : FileAttributeProperty<T>(name, version) {
        override fun readValue(input: DataInputStream): T = read(input)
        override fun writeValue(output: DataOutputStream, value: T) = write(output, value)
    }
}

abstract class FileAttributeProperty<T : Any>(name: String, version: Int, private val default: T? = null) {
    abstract fun readValue(input: DataInputStream): T
    abstract fun writeValue(output: DataOutputStream, value: T)

    private val attribute = FileAttribute(name, version, false)

    @Suppress("LeakingThis")
    private val cache = NullableLazyKey.create(
        "FileAttributeProperty.$name",
        this::computeValue
    )

    private fun computeValue(file: VirtualFile): T? {
        if (file !is VirtualFileWithId || !file.isValid) return null

        return attribute.readAttribute(file)?.use { input ->
            input.readNullable {
                readValue(input)
            }
        } ?: default
    }

    operator fun setValue(file: VirtualFile, property: KProperty<*>, newValue: T?) {
        if (file !is VirtualFileWithId || !file.isValid) return

        attribute.writeAttribute(file).use { output ->
            output.writeNullable(newValue) { value ->
                writeValue(output, value)
            }
        }

        // clear cache
        file.putUserData(cache, null)
    }

    operator fun getValue(file: VirtualFile, property: KProperty<*>): T? =
        cache.getValue(file)
}

fun DataInput.readStringList(): List<String> = readSeq(this) { readString() }
fun DataInput.readFileList(): List<File> = readSeq(this) { readFile() }
fun DataInput.readString(): String = readUTF(this)
fun DataInput.readFile() = File(readUTF(this))

fun DataOutput.writeFileList(iterable: Iterable<File>) = writeSeq(this, iterable.toList()) { writeFile(it) }
fun DataOutput.writeFile(it: File) = writeString(it.canonicalPath)
fun DataOutput.writeString(string: String) = writeUTF(this, string)
fun DataOutput.writeStringList(iterable: Iterable<String>) = writeSeq(this, iterable.toList()) { writeString(it) }

fun <T : Any> DataOutput.writeNullable(nullable: T?, writeT: DataOutput.(T) -> Unit) {
    writeBoolean(nullable != null)
    nullable?.let { writeT(it) }
}

fun <T : Any> DataInput.readNullable(readT: DataInput.() -> T): T? {
    val hasValue = readBoolean()
    return if (hasValue) readT() else null
}