/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.FileAttribute
import org.jetbrains.kotlin.idea.core.util.readString
import org.jetbrains.kotlin.idea.scripting.gradle.importing.KotlinDslScriptModel
import java.io.DataInputStream
import java.io.DataOutputStream

object KotlinDslScriptModels {
    private val attribute = FileAttribute("kotlin-script-dependencies", 1, false)

    fun read(project: Project): List<KotlinDslScriptModel>? {
        return attribute.readAttribute(project.projectFile ?: return null)?.use { readValue(it) }
    }

    fun write(project: Project, models: List<KotlinDslScriptModel>) {
        attribute.writeAttribute(project.projectFile ?: return).use {
            writeValue(it, models)
        }
    }

    fun writeValue(output: DataOutputStream, value: List<KotlinDslScriptModel>) {
        val strings = StringsPool()
        value.forEach {
            strings.getStringId(it.file)
            strings.addStrings(it.classPath)
            strings.addStrings(it.sourcePath)
            strings.addStrings(it.imports)
        }
        strings.freeze()
        strings.writeStrings(output)
        value.forEach {
            strings.writeStringId(it.file, output)
            strings.writeStringIds(it.classPath, output)
            strings.writeStringIds(it.sourcePath, output)
            strings.writeStringIds(it.imports, output)
        }
    }

    fun readValue(input: DataInputStream): List<KotlinDslScriptModel> {
        val strings = StringsPool()
        strings.readStrings(input)
        val n = input.readInt()
        val result = mutableListOf<KotlinDslScriptModel>()
        repeat(n) {
            result.add(
                KotlinDslScriptModel(
                    strings.readStringId(input),
                    strings.readStringIds(input),
                    strings.readStringIds(input),
                    strings.readStringIds(input),
                    listOf()
                )
            )
        }
        return result
    }
}

private class StringsPool {
    var freeze = false
    val strings = mutableListOf<String>()
    val ids = mutableMapOf<String, Int>()

    fun getString(id: Int) = strings[id]

    fun getStringId(string: String): Int = ids.getOrPut(string) {
        check(!freeze)
        val id = strings.size
        strings.add(string)
        id
    }

    fun addString(string: String) {
        getStringId(string)
    }

    fun addStrings(list: List<String>) {
        list.forEach { getStringId(it) }
    }

    fun freeze() {
        // sort for optimal performance and compression
        strings.sort()
        strings.forEachIndexed { index, s ->
            ids[s] = index
        }

        freeze = true
    }

    fun writeStringIds(strings: List<String>, output: DataOutputStream) {
        output.writeInt(strings.size)
        strings.forEach {
            writeStringId(it, output)
        }
    }

    fun writeStringId(it: String, output: DataOutputStream) {
        output.writeInt(getStringId(it))
    }

    fun readStringIds(input: DataInputStream): List<String> {
        val n = input.readInt()
        val result = ArrayList<String>(n)
        repeat(n) {
            result.add(readStringId(input))
        }
        return result
    }

    fun readStringId(input: DataInputStream) = getString(input.readInt())

    fun writeStrings(output: DataOutputStream) {
        output.writeInt(ids.size)
        ids.keys.forEach { output.writeChars(it) }
    }

    fun readStrings(input: DataInputStream) {
        repeat(input.readInt()) {
            ids[input.readString()] = ids.size
        }
    }
}