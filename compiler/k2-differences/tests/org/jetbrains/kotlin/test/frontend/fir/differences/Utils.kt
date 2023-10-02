/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package org.jetbrains.kotlin.test.frontend.fir.differences

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.Closeable
import java.io.File

inline fun <reified T> deserializeOrGenerate(file: File, block: () -> T): T {
    val save: (T) -> Unit = {
        file.outputStream().use { stream ->
            Json.encodeToStream(it, stream)
        }
    }

    return if (file.isFile) {
        try {
            file.inputStream().use { stream ->
                Json.decodeFromStream<T>(stream)
            }
        } catch (e: SerializationException) {
            println("> Couldn't deserialize ${file.name}, regenerating...")
            block().also(save)
        } catch (e: IllegalArgumentException) {
            println("> Couldn't deserialize ${file.name}, regenerating...")
            block().also(save)
        }
    } else {
        block().also(save)
    }
}

inline fun deserializeIfSameHashOrGenerate(file: File, block: () -> TestsCollection): TestsCollection {
    val existing = deserializeOrGenerate(file, block)

    return when {
        existing.commitHash != currentCommitHash -> block()
        else -> existing
    }
}

fun File.child(name: String) = File(path + File.separator + name)

fun File.forEachChildRecursively(
    shouldIgnoreDirectory: (File) -> Boolean = { false },
    action: (File) -> Unit,
) {
    val children = listFiles() ?: error("Couldn't list the files inside $this")

    for (it in children) {
        when {
            it.isDirectory -> when {
                !shouldIgnoreDirectory(it) -> it.forEachChildRecursively(shouldIgnoreDirectory, action)
            }
            else -> action(it)
        }
    }
}

fun useAll(vararg closeables: Closeable?, block: () -> Unit) {
    val usages = mutableListOf(block)

    for (it in closeables.indices) {
        usages += {
            closeables[it].use { _ ->
                usages[it]()
            }
        }
    }

    usages.last()()
}

fun diagnosticsWithinRequest(url: String): Set<String> {
    val response = getJson(url, API_HEADERS)

    return Regex("""summary":"[^"]*\s(\w+)""")
        .findAll(response)
        .map { it.groupValues.last() }
        .toSet()
}

val currentCommitHash by lazy {
    Runtime.getRuntime().exec("git rev-parse HEAD").inputStream.reader().readText().trim()
}
