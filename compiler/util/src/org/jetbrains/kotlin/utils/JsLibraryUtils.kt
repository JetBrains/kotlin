/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.utils

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.Processor
import org.jetbrains.kotlin.utils.fileUtils.withReplacedExtensionOrNull
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

object JsLibraryUtils {
    private val LOG = Logger.getInstance(LibraryUtils::class.java)

    private val META_INF_RESOURCES = "${LibraryUtils.META_INF}resources/"

    @JvmStatic fun copyJsFilesFromLibraries(libraries: List<String>, outputLibraryJsPath: String) {
        for (library in libraries) {
            val file = File(library)
            assert(file.exists()) { "Library $library not found" }

            if (file.isDirectory) {
                copyJsFilesFromDirectory(file, outputLibraryJsPath)
            }
            else {
                copyJsFilesFromZip(file, outputLibraryJsPath)
            }
        }
    }

    @JvmStatic fun traverseJsLibraries(libs: List<File>, action: (content: String, path: String) -> Unit) {
        libs.forEach { traverseJsLibrary(it, action) }
    }

    @JvmStatic fun traverseJsLibrary(lib: File, action: (content: String, path: String) -> Unit) {
        when {
            lib.isDirectory -> traverseDirectory(lib, action)
            FileUtil.isJarOrZip(lib) -> traverseArchive(lib, action)
            lib.name.endsWith(KotlinJavascriptMetadataUtils.JS_EXT) -> {
                lib.runIfFileExists(action)
                val jsFile = lib.withReplacedExtensionOrNull(
                        KotlinJavascriptMetadataUtils.META_JS_SUFFIX, KotlinJavascriptMetadataUtils.JS_EXT
                )
                jsFile?.runIfFileExists(action)
            }
            else -> throw IllegalArgumentException("Unknown library format (directory, zip or js file expected): $lib")
        }
    }

    private fun File.runIfFileExists(action: (content: String, path: String) -> Unit) {
        if (isFile) {
            action(FileUtil.loadFile(this), "")
        }
    }

    private fun copyJsFilesFromDirectory(dir: File, outputLibraryJsPath: String) {
        traverseDirectory(dir) { content, relativePath ->
            FileUtil.writeToFile(File(outputLibraryJsPath, relativePath), content)
        }
    }

    private fun processDirectory(dir: File, action: (content: String, relativePath: String) -> Unit) {
        FileUtil.processFilesRecursively(dir, Processor<File> { file ->
            val relativePath = FileUtil.getRelativePath(dir, file)
                               ?: throw IllegalArgumentException("relativePath should not be null $dir $file")
            if (file.isFile && relativePath.endsWith(KotlinJavascriptMetadataUtils.JS_EXT)) {
                val suggestedRelativePath = getSuggestedPath(relativePath) ?: return@Processor true
                action(FileUtil.loadFile(file), suggestedRelativePath)
            }
            true
        })
    }

    private fun traverseDirectory(dir: File, action: (content: String, relativePath: String) -> Unit) {
        try {
            processDirectory(dir, action)
        }
        catch (ex: IOException) {
            LOG.error("Could not read files from directory ${dir.name}: ${ex.message}")
        }
    }

    private fun copyJsFilesFromZip(file: File, outputLibraryJsPath: String) {
        traverseArchive(file) { content, relativePath ->
            FileUtil.writeToFile(File(outputLibraryJsPath, relativePath), content)
        }
    }

    private fun traverseArchive(file: File, action: (content: String, relativePath: String) -> Unit) {
        val zipFile = ZipFile(file.path)
        try {
            val zipEntries = zipFile.entries()
            while (zipEntries.hasMoreElements()) {
                val entry = zipEntries.nextElement()
                val entryName = entry.name
                if (!entry.isDirectory && entryName.endsWith(KotlinJavascriptMetadataUtils.JS_EXT)) {
                    val relativePath = getSuggestedPath(entryName) ?: continue

                    val stream = zipFile.getInputStream(entry)
                    val content = FileUtil.loadTextAndClose(stream)
                    action(content, relativePath)
                }
            }
        }
        catch (ex: IOException) {
            LOG.error("Could not extract files from archive ${file.name}: ${ex.message}")
        }
        finally {
            zipFile.close()
        }
    }

    private fun getSuggestedPath(path: String): String? {
        val systemIndependentPath = FileUtil.toSystemIndependentName(path)
        if (systemIndependentPath.startsWith(LibraryUtils.META_INF)) {
            if (systemIndependentPath.startsWith(META_INF_RESOURCES)) {
                return path.substring(META_INF_RESOURCES.length)
            }
            return null
        }

        return path
    }
}
