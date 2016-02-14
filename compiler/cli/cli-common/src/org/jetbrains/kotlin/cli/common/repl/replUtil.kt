/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cli.common.repl

import com.google.common.base.Throwables
import java.io.File
import java.net.URLClassLoader

fun makeScriptBaseName(codeLine: ReplCodeLine) =
        "Line_${codeLine.no}${if (codeLine.generation > REPL_CODE_LINE_FIRST_GEN) "_gen_${codeLine.generation}" else ""}"

fun renderReplStackTrace(cause: Throwable, startFromMethodName: String): String {
    val newTrace = arrayListOf<StackTraceElement>()
    var skip = true
    for (element in cause.stackTrace.reversed()) {
        if ("${element.className}.${element.methodName}" == startFromMethodName) {
            skip = false
        }
        if (!skip) {
            newTrace.add(element)
        }
    }

    val resultingTrace = newTrace.reversed().dropLast(1)

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "UsePropertyAccessSyntax")
    (cause as java.lang.Throwable).setStackTrace(resultingTrace.toTypedArray())

    return Throwables.getStackTraceAsString(cause).trimEnd()
}

internal fun ClassLoader.listAllUrlsAsFiles(): List<File> {
    val parents = generateSequence(this) { loader -> loader.parent }.filterIsInstance(URLClassLoader::class.java)
    return parents.fold(emptyList<File>()) { accum, loader ->
        loader.listLocalUrlsAsFiles() + accum
    }.distinct()
}

internal fun URLClassLoader.listLocalUrlsAsFiles(): List<File> {
    return this.urLs.map { it.toString().removePrefix("file:") }.filterNotNull().map(::File)
}

internal fun <T : Any> List<T>.ensureNotEmpty(error: String): List<T> {
    if (this.isEmpty()) throw IllegalStateException(error)
    return this
}