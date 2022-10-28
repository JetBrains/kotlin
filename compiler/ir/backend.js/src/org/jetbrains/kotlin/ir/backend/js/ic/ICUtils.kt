/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.protobuf.CodedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

internal inline fun <T> File.ifExists(f: File.() -> T): T? = if (exists()) f() else null

internal fun File.recreate() {
    if (exists()) {
        delete()
    } else {
        parentFile?.mkdirs()
    }
    createNewFile()
}

internal inline fun <T> File.useCodedInputIfExists(f: CodedInputStream.() -> T) = ifExists {
    FileInputStream(this).use {
        CodedInputStream.newInstance(it).f()
    }
}

internal inline fun File.useCodedOutput(f: CodedOutputStream.() -> Unit) {
    parentFile?.mkdirs()
    recreate()
    FileOutputStream(this).use {
        val out = CodedOutputStream.newInstance(it)
        out.f()
        out.flush()
    }
}

internal fun icError(what: String, libFile: KotlinLibraryFile? = null, srcFile: KotlinSourceFile? = null): Nothing {
    val filePath = listOfNotNull(libFile?.path, srcFile?.path).joinToString(":") { File(it).name }
    val msg = if (filePath.isEmpty()) what else "$what for $filePath"
    error("IC internal error: $msg")
}

internal fun notFoundIcError(what: String, libFile: KotlinLibraryFile? = null, srcFile: KotlinSourceFile? = null): Nothing {
    icError("can not find $what", libFile, srcFile)
}

internal inline fun <E> buildListUntil(to: Int, builderAction: MutableList<E>.(Int) -> Unit): List<E> {
    return buildList(to) { repeat(to) { builderAction(it) } }
}

internal inline fun <E> buildSetUntil(to: Int, builderAction: MutableSet<E>.(Int) -> Unit): Set<E> {
    return HashSet<E>(to).apply { repeat(to) { builderAction(it) } }
}

internal inline fun <K, V> buildMapUntil(to: Int, builderAction: MutableMap<K, V>.(Int) -> Unit): Map<K, V> {
    return HashMap<K, V>(to).apply { repeat(to) { builderAction(it) } }
}

internal class StopwatchIC {
    private var lapStart: Long = 0
    private var lapDescription: String? = null

    private val lapsImpl = mutableListOf<Pair<String, Long>>()

    val laps: List<Pair<String, Long>>
        get() = lapsImpl

    fun startNext(description: String) {
        val now = System.nanoTime()
        stop(now)
        lapDescription = description
        lapStart = now
    }

    fun stop(stopTime: Long? = null) {
        lapDescription?.let { description ->
            lapsImpl += description to ((stopTime ?: System.nanoTime()) - lapStart)
        }
        lapDescription = null
    }
}
