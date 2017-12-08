/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cli.jvm.repl

import com.google.common.hash.Hashing
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtPsiFactory
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.jar.JarFile

class KotlinLibraryNameCache(val project: Project) {
    private companion object {
        val cacheDirectory = File(
                System.getProperty("user.home")
                ?: error("Can't find home directory"), ".kotlin/repl/libraryCache"
        ).apply { mkdirs() }
    }

    val ktFactory = KtPsiFactory(project)

    private val librariesWithSources = mutableMapOf<File, Boolean>()
    private val libraryCache = mutableMapOf<File, LibraryNames>()

    fun getEntryName(library: File, packageName: String, declarationName: String): String? {
        processAndCacheIfNeeded(library)

        val libraryNames = libraryCache[library] ?: return null
        return (libraryNames[packageName] ?: return null)[declarationName]
    }

    fun hasSources(library: File): Boolean {
        processAndCacheIfNeeded(library)

        return librariesWithSources[library] ?: false
    }

    private fun processAndCacheIfNeeded(library: File) {
        val cacheFile = File(cacheDirectory, getFileIdentifier(library))
        if (!cacheFile.exists()) {
            processAndCache(library)
        } else if (library !in libraryCache) {
            loadLibraryFromCache(library, cacheFile)
        }
    }

    private fun loadLibraryFromCache(library: File, cacheFile: File) {
        val (sourceFilesFound, libraryNames) = cacheFile.inputStream().use {
            ObjectInputStream(it).run {
                @Suppress("UNCHECKED_CAST")
                Pair(readBoolean(), readObject() as? LibraryNames)
            }
        }

        libraryCache[library] = libraryNames ?: emptyMap()
        librariesWithSources[library] = sourceFilesFound
    }

    private fun processAndCache(library: File): LibraryNames {
        if (!library.isFile) {
            return emptyMap()
        }

        try {
            val namesForFile: MutableLibraryNames = mutableMapOf()
            val sourceFilesFound = processLibrary(library, namesForFile)

            libraryCache[library] = namesForFile
            librariesWithSources[library] = sourceFilesFound

            File(cacheDirectory, getFileIdentifier(library)).outputStream().use { os ->
                ObjectOutputStream(os).use { oos ->
                    oos.writeBoolean(sourceFilesFound)
                    oos.writeObject(namesForFile)
                }
            }

            return namesForFile
        } catch (e: Throwable) {
            throw RuntimeException("Can not process $library", e)
        }
    }

    private fun getFileIdentifier(file: File): String {
        return sha1(file.absolutePath + file.length() + file.lastModified())
    }

    private fun processLibrary(file: File, consumer: MutableLibraryNames): Boolean {
        var sourceFilesFound = false

        JarFile(file).use { jar ->
            for (entry in jar.entries()) {
                val extension = entry.name.substringAfterLast('.', missingDelimiterValue = "")
                if (!sourceFilesFound && (extension == "kt") || extension == "java") {
                    sourceFilesFound = true
                }

                fun readEntryContents() = jar.getInputStream(entry).reader().readText()

                if (extension == "kt") {
                    val (packageName, declarationNames) = scanKotlinSource(readEntryContents())
                    if (declarationNames.isEmpty()) {
                        continue
                    }

                    val packageData = consumer.getOrPut(packageName, { mutableMapOf() })
                    declarationNames.forEach { packageData[it] = entry.name }
                }
            }
        }

        return sourceFilesFound
    }

    private fun scanKotlinSource(source: String): FileNames {
        val ktFile = ktFactory.createFile(source)

        val declarationNames = hashSetOf<String>()

        fun processDeclaration(prefix: String, declaration: KtDeclaration) {
            if (declaration.docComment == null) {
                return
            }

            declarationNames += prefix + declaration.name

            if (declaration is KtClassOrObject) {
                val newPrefix = prefix + declaration.name + "."
                declaration.declarations.forEach { processDeclaration(newPrefix, it) }
            }
        }

        ktFile.declarations.forEach { processDeclaration("", it) }
        return FileNames(ktFile.packageFqName.asString(), declarationNames)
    }
}

private typealias LibraryNames = Map<String, Map<String, String>>
private typealias MutableLibraryNames = MutableMap<String, MutableMap<String, String>>

private data class FileNames(val packageName: String, val declarationNames: Set<String>)

private fun sha1(text: String): String {
    return Hashing.sha1().hashBytes(text.toByteArray()).toString()
}