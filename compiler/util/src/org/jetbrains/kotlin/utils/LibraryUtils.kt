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
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Processor
import java.io.*
import java.util.Properties
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.Manifest
import java.util.zip.ZipFile
import kotlin.platform.platformStatic

public object LibraryUtils {
    private val LOG = Logger.getInstance(javaClass<LibraryUtils>())

    public val KOTLIN_JS_MODULE_NAME: String = "Kotlin-JS-Module-Name"
    private var TITLE_KOTLIN_JAVASCRIPT_STDLIB: String
    private var TITLE_KOTLIN_JAVASCRIPT_LIB: String
    private val METAINF = "META-INF/"
    private val MANIFEST_PATH = "${METAINF}MANIFEST.MF"
    private val METAINF_RESOURCES = "${METAINF}resources/"
    private val KOTLIN_JS_MODULE_ATTRIBUTE_NAME = Attributes.Name(KOTLIN_JS_MODULE_NAME);

    {
        var jsStdLib = ""
        var jsLib = ""

        val manifestProperties = javaClass<LibraryUtils>().getResourceAsStream("/kotlinManifest.properties")
        if (manifestProperties != null) {
            try {
                val properties = Properties()
                properties.load(manifestProperties)
                jsStdLib = properties.getPropertyOrFail("manifest.impl.title.kotlin.javascript.stdlib")
                jsLib = properties.getPropertyOrFail("manifest.spec.title.kotlin.javascript.lib")
            }
            catch (e: IOException) {
                LOG.error(e)
            }

        }
        else {
            LOG.error("Resource 'kotlinManifest.properties' not found.")
        }

        TITLE_KOTLIN_JAVASCRIPT_STDLIB = jsStdLib
        TITLE_KOTLIN_JAVASCRIPT_LIB = jsLib
    }

    platformStatic
    public fun getJarFile(classesRoots: List<VirtualFile>, jarName: String): VirtualFile? {
        for (root in classesRoots) {
            if (root.getName() == jarName) {
                return root
            }
        }

        return null
    }

    platformStatic
    public fun getKotlinJsModuleName(library: File): String? {
        return getManifestMainAttributesFromJarOrDirectory(library)?.getValue(KOTLIN_JS_MODULE_ATTRIBUTE_NAME)
    }

    platformStatic
    public fun isOldKotlinJavascriptLibrary(library: File): Boolean =
            checkAttributeValue(library, TITLE_KOTLIN_JAVASCRIPT_LIB, Attributes.Name.SPECIFICATION_TITLE) && getKotlinJsModuleName(library) != null

    platformStatic
    public fun isKotlinJavascriptLibraryWithMetadata(library: File): Boolean = KotlinJavascriptMetadataUtils.loadMetadata(library).isNotEmpty()

    platformStatic
    public fun isKotlinJavascriptLibrary(library: File): Boolean =
            isOldKotlinJavascriptLibrary(library) || isKotlinJavascriptLibraryWithMetadata(library)

    platformStatic
    public fun isKotlinJavascriptStdLibrary(library: File): Boolean {
        return checkAttributeValue(library, TITLE_KOTLIN_JAVASCRIPT_STDLIB, Attributes.Name.IMPLEMENTATION_TITLE)
    }

    platformStatic
    public fun copyJsFilesFromLibraries(libraries: List<String>, outputLibraryJsPath: String) {
        for (library in libraries) {
            val file = File(library)
            assert(file.exists()) { "Library " + library + " not found" }

            if (file.isDirectory()) {
                copyJsFilesFromDirectory(file, outputLibraryJsPath)
            }
            else {
                copyJsFilesFromZip(file, outputLibraryJsPath)
            }
        }
    }

    platformStatic
    public fun readJsFiles(libraries: List<String>): List<String> {
        val files = arrayListOf<String>()
        val libs = libraries.map { File(it) }.filter { it.exists() }

        for (lib in libs) {
            when {
                lib.isDirectory() ->
                    traverseDirectory(lib) { (file, path) ->
                        files.add(FileUtil.loadFile(file))
                    }
                FileUtil.isJarOrZip(lib) ->
                    traverseArchive(lib) { (content, path) ->
                        files.add(content)
                    }
                else ->
                    throw IllegalArgumentException("Unknown library format (directory or zip expected): $lib")
            }
        }

        return files
    }

    private fun copyJsFilesFromDirectory(dir: File, outputLibraryJsPath: String) {
        traverseDirectory(dir) {
            file, relativePath -> FileUtil.copy(file, File(outputLibraryJsPath, relativePath))
        }
    }

    private fun processDirectory(dir: File, action: (File, relativePath: String) -> Unit) {
        FileUtil.processFilesRecursively(dir, object : Processor<File> {
            override fun process(file: File): Boolean {
                val relativePath = FileUtil.getRelativePath(dir, file) ?: throw IllegalArgumentException("relativePath should not be null " + dir + " " + file)
                if (file.isFile() && relativePath.endsWith(KotlinJavascriptMetadataUtils.JS_EXT)) {
                    val suggestedRelativePath = getSuggestedPath(relativePath)
                    if (suggestedRelativePath == null) return true

                    action(file, suggestedRelativePath)
                }
                return true
            }
        })
    }

    fun traverseDirectory(dir: File, action: (File, relativePath: String) -> Unit) {
        try {
            processDirectory(dir, action)
        }
        catch (ex: IOException) {
            LOG.error("Could not read files from directory ${dir.getName()}: ${ex.getMessage()}")
        }
    }

    private fun copyJsFilesFromZip(file: File, outputLibraryJsPath: String) {
        traverseArchive(file) { content, relativePath ->
            FileUtil.writeToFile(File(outputLibraryJsPath, relativePath), content)
        }
    }

    fun traverseArchive(file: File, action: (content: String, relativePath: String) -> Unit) {
        val zipFile = ZipFile(file.getPath())
        try {
            val zipEntries = zipFile.entries()
            while (zipEntries.hasMoreElements()) {
                val entry = zipEntries.nextElement()
                val entryName = entry.getName()
                if (!entry.isDirectory() && entryName.endsWith(KotlinJavascriptMetadataUtils.JS_EXT)) {
                    val relativePath = getSuggestedPath(entryName)
                    if (relativePath == null) continue

                    val stream = zipFile.getInputStream(entry)
                    val content = FileUtil.loadTextAndClose(stream)
                    action(content, relativePath)
                }
            }
        }
        catch (ex: IOException) {
            LOG.error("Could not extract files from archive ${file.getName()}: ${ex.getMessage()}")
        }
        finally {
            zipFile.close()
        }
    }

    private fun getSuggestedPath(path: String): String? {
        val systemIndependentPath = FileUtil.toSystemIndependentName(path)
        if (systemIndependentPath.startsWith(METAINF)) {
            if (systemIndependentPath.startsWith(METAINF_RESOURCES)) {
                return path.substring(METAINF_RESOURCES.length())
            }
            return null
        }

        return path
    }

    private fun getManifestFromJar(library: File): Manifest? {
        if (!library.canRead()) return null

        try {
            val jarFile = JarFile(library)
            try {
                return jarFile.getManifest()
            }
            finally {
                jarFile.close()
            }
        }
        catch (ignored: IOException) {
            return null
        }
    }

    private fun getManifestFromDirectory(library: File): Manifest? {
        if (!library.canRead() || !library.isDirectory()) return null

        val manifestFile = File(library, MANIFEST_PATH)
        if (!manifestFile.exists()) return null

        try {
            val inputStream = FileInputStream(manifestFile)
            try {
                return Manifest(inputStream)
            }
            finally {
                inputStream.close()
            }
        }
        catch (ignored: IOException) {
            LOG.warn("IOException " + ignored)
            return null
        }
    }

    private fun getManifestFromJarOrDirectory(library: File): Manifest? =
            if (library.isDirectory()) getManifestFromDirectory(library) else getManifestFromJar(library)

    private fun getManifestMainAttributesFromJarOrDirectory(library: File): Attributes? =
            getManifestFromJarOrDirectory(library)?.getMainAttributes()

    private fun checkAttributeValue(library: File, expected: String, attributeName: Attributes.Name): Boolean {
        val attributes = getManifestMainAttributesFromJarOrDirectory(library)
        val value = attributes?.getValue(attributeName)
        return value != null && value == expected
    }

    private fun Properties.getPropertyOrFail(propName: String): String {
        val value = getProperty(propName)

        if (value == null) {
            val bytes = ByteArrayOutputStream()
            list(PrintStream(bytes))
            LOG.error("$propName not found.\n $bytes")
        }

        return value
    }
}
