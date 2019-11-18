/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.runner

import java.io.File
import java.net.URL

fun findCompilerJar(classFromJarInTheSameLocation: Class<*>, kotlinHome: File): List<File> {
    val baseDir = (tryGetResourcePathForClass(classFromJarInTheSameLocation)?.parentFile ?: kotlinHome).takeIf { it.isDirectory }
        ?: return emptyList()
    val compilerJars = baseDir.listFiles { f: File ->
        COMPILER_JARS.any { expected ->
            f.matchMaybeVersionedFile(expected) && f.extension == "jar"
        }
    }?.takeIf { it.size >= COMPILER_JARS.size }?.toList()
    return compilerJars ?: emptyList()
}

private val COMPILER_JARS = listOf("kotlin-compiler", "kotlin-stdlib", "kotlin-reflect")

// below is a copy from kotlin.script.experimental.jvm.impl, but we do not want to introduce dependency to that implementation, and
// there is no other good place for sharing this functionality yet
// TODO: find a good place and put the shared code into it

internal fun tryGetResourcePathForClass(aClass: Class<*>): File? {
    val path = "/" + aClass.name.replace('.', '/') + ".class"
    return getResourceRoot(aClass, path)?.let {
        File(it).absoluteFile
    }
}

private fun getResourceRoot(context: Class<*>, path: String): String? {
    var url: URL? = context.getResource(path)
    if (url == null) {
        url = ClassLoader.getSystemResource(path.substring(1))
    }
    return if (url != null) extractRoot(url, path) else null
}

private const val JAR_PROTOCOL = "jar"
private const val FILE_PROTOCOL = "file"
private const val JAR_SEPARATOR = "!/"
private const val SCHEME_SEPARATOR = "://"

private fun extractRoot(resourceURL: URL, resourcePath: String): String? {
    if (!resourcePath.startsWith('/') || resourcePath.startsWith('\\')) return null

    var resultPath: String? = null
    val protocol = resourceURL.protocol
    if (protocol == FILE_PROTOCOL) {
        val path = resourceURL.toFileOrNull()!!.path
        val testPath = path.replace('\\', '/')
        val testResourcePath = resourcePath.replace('\\', '/')
        if (testPath.endsWith(testResourcePath, ignoreCase = true)) {
            resultPath = path.substring(0, path.length - resourcePath.length)
        }
    } else if (protocol == JAR_PROTOCOL) {
        val paths = splitJarUrl(resourceURL.file)
        if (paths?.first != null) {
            resultPath = File(paths.first).canonicalPath
        }
    }

    return resultPath?.trimEnd(File.separatorChar)
}

private fun splitJarUrl(url: String): Pair<String, String>? {
    val pivot = url.indexOf(JAR_SEPARATOR).takeIf { it >= 0 } ?: return null

    val resourcePath = url.substring(pivot + 2)
    var jarPath = url.substring(0, pivot)

    if (jarPath.startsWith(JAR_PROTOCOL + ":")) {
        jarPath = jarPath.substring(JAR_PROTOCOL.length + 1)
    }

    if (jarPath.startsWith(FILE_PROTOCOL)) {
        try {
            jarPath = URL(jarPath).toFileOrNull()!!.path.replace('\\', '/')
        } catch (e: Exception) {
            jarPath = jarPath.substring(FILE_PROTOCOL.length)
            if (jarPath.startsWith(SCHEME_SEPARATOR)) {
                jarPath = jarPath.substring(SCHEME_SEPARATOR.length)
            } else if (jarPath.startsWith(':')) {
                jarPath = jarPath.substring(1)
            }
        }

    }
    return Pair(jarPath, resourcePath)
}

private fun URL.toFileOrNull() =
    try {
        File(toURI())
    } catch (e: IllegalArgumentException) {
        null
    } catch (e: java.net.URISyntaxException) {
        null
    } ?: run {
        if (protocol != "file") null
        else File(file)
    }

private fun File.matchMaybeVersionedFile(baseName: String) =
    name == baseName ||
            name == baseName.removeSuffix(".jar") || // for classes dirs
            Regex(Regex.escape(baseName.removeSuffix(".jar")) + "(-\\d.*)?\\.jar").matches(name)
