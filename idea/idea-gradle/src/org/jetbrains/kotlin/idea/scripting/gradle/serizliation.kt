/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.FileAttribute
import org.jetbrains.kotlin.idea.core.util.readString
import org.jetbrains.kotlin.idea.core.util.writeString
import org.jetbrains.kotlin.idea.scripting.gradle.importing.KotlinDslScriptModel
import java.io.DataInputStream
import java.io.DataOutputStream

object KotlinDslScriptModels {
    private val attribute = FileAttribute("kotlin-script-dependencies", 1, false)

    fun read(project: Project): List<KotlinDslScriptModel>? {
        return attribute.readAttribute(project.projectFile ?: return null)?.use { readKotlinDslScriptModels(it) }
    }

    fun write(project: Project, models: List<KotlinDslScriptModel>) {
        attribute.writeAttribute(project.projectFile ?: return).use {
            writeKotlinDslScriptModels(it, models)
        }
    }
}

fun writeKotlinDslScriptModels(output: DataOutputStream, value: List<KotlinDslScriptModel>) {
    val strings = StringsPool.writer(output)
    value.forEach {
        strings.addString(it.file)
        strings.addStrings(it.classPath)
        strings.addStrings(it.sourcePath)
        strings.addStrings(it.imports)
    }
    strings.writeHeader()
    output.writeList(value) {
        strings.writeStringId(it.file)
        output.writeString(it.inputs.sections)
        output.writeLong(it.inputs.inputsTS)
        strings.writeStringIds(it.classPath)
        strings.writeStringIds(it.sourcePath)
        strings.writeStringIds(it.imports)
    }
}

fun readKotlinDslScriptModels(input: DataInputStream): List<KotlinDslScriptModel> {
    val strings = StringsPool.reader(input)
    return input.readList {
        KotlinDslScriptModel(
            strings.readString(),
            GradleKotlinScriptConfigurationInputs(input.readString(), input.readLong()),
            strings.readStrings(),
            strings.readStrings(),
            strings.readStrings(),
            listOf()
        )
    }
}

private object StringsPool {
    fun writer(output: DataOutputStream) = Writer(output)

    class Writer(val output: DataOutputStream) {
        var freeze = false
        val ids = mutableMapOf<String, Int>()

        fun getStringId(string: String) = ids.getOrPut(string) {
            check(!freeze)
            ids.size
        }

        fun addString(string: String) {
            getStringId(string)
        }

        fun addStrings(list: List<String>) {
            list.forEach { addString(it) }
        }

        fun writeHeader() {
            freeze = true

            output.writeInt(ids.size)

            // sort for optimal performance and compression
            ids.keys.sorted().forEachIndexed { index, s ->
                ids[s] = index
                output.writeString(s)
            }
        }

        fun writeStringId(it: String) {
            output.writeInt(getStringId(it))
        }

        fun writeStringIds(strings: List<String>) {
            output.writeInt(strings.size)
            strings.forEach {
                writeStringId(it)
            }
        }
    }

    fun reader(input: DataInputStream): Reader {
        val strings = input.readList { input.readString() }
        return Reader(input, strings)
    }

    class Reader(val input: DataInputStream, val strings: List<String>) {
        fun getString(id: Int) = strings[id]

        fun readString() = getString(input.readInt())

        fun readStrings(): List<String> = input.readList { readString() }
    }
}

private inline fun <T> DataOutputStream.writeList(list: List<T>, write: (T) -> Unit) {
    writeInt(list.size)
    list.forEach { write(it) }
}

private inline fun <T> DataInputStream.readList(read: () -> T): List<T> {
    val n = readInt()
    val result = ArrayList<T>(n)
    repeat(n) {
        result.add(read())
    }
    return result
}