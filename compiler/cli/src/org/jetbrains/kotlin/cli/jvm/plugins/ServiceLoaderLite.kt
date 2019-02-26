/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.plugins

import java.io.File
import java.io.IOError
import java.lang.Character.isJavaIdentifierPart
import java.lang.Character.isJavaIdentifierStart
import java.net.URLClassLoader
import java.nio.file.FileSystemNotFoundException
import java.nio.file.Paths
import java.util.zip.ZipFile

/**
 * ServiceLoader has a file handle leak in JDK8: https://bugs.openjdk.java.net/browse/JDK-8156014.
 * This class, hopefully, doesn't. :)
 */
object ServiceLoaderLite {
    private const val SERVICE_DIRECTORY_LOCATION = "META-INF/services/"

    class ServiceLoadingException(val file: File, cause: Throwable) : RuntimeException("Error loading services from $file", cause)

    /**
     * Returns implementations for the given `service` declared in META-INF/services of the `classLoader` roots.
     *
     * Note that the behavior is radically different from what Java ServiceLoader does.
     * ServiceLoaderLite doesn't iterate over the whole ClassLoader hierarchy, it takes only the immediate roots of `classLoader`.
     * In fact, this is often the desired behavior.
     */
    fun <Service> loadImplementations(service: Class<out Service>, classLoader: URLClassLoader): List<Service> {
        val files = classLoader.urLs.map { url ->
            try {
                Paths.get(url.toURI()).toFile()
            } catch (e: FileSystemNotFoundException) {
                throw IllegalArgumentException("Only local URLs are supported, got ${url.protocol}")
            } catch (e: UnsupportedOperationException) {
                throw IllegalArgumentException("Only local URLs are supported, got ${url.protocol}")
            }
        }

        return loadImplementations(service, files, classLoader)
    }

    fun <Service> loadImplementations(service: Class<out Service>, files: List<File>, classLoader: ClassLoader): MutableList<Service> {
        val implementations = mutableListOf<Service>()

        for (className in findImplementations(service, files)) {
            val instance = Class.forName(className, false, classLoader).newInstance()
            implementations += service.cast(instance)
        }

        return implementations
    }

    inline fun <reified Service : Any> findImplementations(files: List<File>): Set<String> {
        return findImplementations(Service::class.java, files)
    }

    inline fun <reified Service : Any> loadImplementations(classLoader: URLClassLoader): List<Service> {
        return loadImplementations(Service::class.java, classLoader)
    }

    fun findImplementations(service: Class<*>, files: List<File>): Set<String> {
        return files.flatMapTo(linkedSetOf()) { findImplementations(service, it) }
    }

    private fun findImplementations(service: Class<*>, file: File): Set<String> {
        val classIdentifier = getClassIdentifier(service)

        return when {
            file.isDirectory -> findImplementationsInDirectory(classIdentifier, file)
            file.isFile && file.extension.toLowerCase() == "jar" -> findImplementationsInJar(classIdentifier, file)
            else -> emptySet()
        }
    }

    private fun findImplementationsInDirectory(classId: String, file: File): Set<String> {
        val serviceFile = File(file, SERVICE_DIRECTORY_LOCATION + classId).takeIf { it.isFile } ?: return emptySet()

        try {
            return serviceFile.useLines { parseLines(file, it) }
        } catch (e: IOError) {
            throw ServiceLoadingException(file, e)
        }
    }

    private fun findImplementationsInJar(classId: String, file: File): Set<String> {
        ZipFile(file).use { zipFile ->
            val entry = zipFile.getEntry(SERVICE_DIRECTORY_LOCATION + classId) ?: return emptySet()
            zipFile.getInputStream(entry).use { inputStream ->
                return inputStream.bufferedReader().useLines { parseLines(file, it) }
            }
        }
    }

    private fun parseLines(file: File, lines: Sequence<String>): Set<String> {
        return lines.mapNotNullTo(linkedSetOf()) { parseLine(file, it) }
    }

    private fun parseLine(file: File, line: String): String? {
        val actualLine = line.substringBefore('#').trim().takeIf { it.isNotEmpty() } ?: return null

        actualLine.forEachIndexed { index: Int, c: Char ->
            val isValid = if (index == 0) isJavaIdentifierStart(c) else isJavaIdentifierPart(c) || c == '.'
            if (!isValid) {
                val errorText = "Invalid Java identifier: $line"
                throw ServiceLoadingException(file, RuntimeException(errorText))
            }
        }

        return actualLine
    }

    private fun getClassIdentifier(service: Class<*>): String {
        return service.name
    }
}