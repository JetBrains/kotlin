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
import com.intellij.openapi.vfs.VirtualFile
import java.io.*
import java.util.*
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.Manifest

object LibraryUtils {
    private val LOG = Logger.getInstance(LibraryUtils::class.java)

    private var TITLE_KOTLIN_JAVASCRIPT_STDLIB: String

    val META_INF = "META-INF/"
    private val MANIFEST_PATH = "${META_INF}MANIFEST.MF"

    init {
        var jsStdLib = ""

        val manifestProperties = LibraryUtils::class.java.getResourceAsStream("/kotlinManifest.properties")
        if (manifestProperties != null) {
            try {
                val properties = Properties()
                properties.load(manifestProperties)
                jsStdLib = properties.getPropertyOrFail("manifest.impl.title.kotlin.javascript.stdlib")
            }
            catch (e: IOException) {
                LOG.error(e)
            }

        }
        else {
            LOG.error("Resource 'kotlinManifest.properties' not found.")
        }

        TITLE_KOTLIN_JAVASCRIPT_STDLIB = jsStdLib
    }

    @JvmStatic fun getJarFile(classesRoots: List<VirtualFile>, jarName: String): VirtualFile? {
        return classesRoots.firstOrNull { it.name == jarName }
    }

    @Suppress("unused") // used in K2JSCompilerMojo
    @JvmStatic fun isKotlinJavascriptLibrary(library: File): Boolean = KotlinJavascriptMetadataUtils.loadMetadata(library).isNotEmpty()

    @JvmStatic fun isKotlinJavascriptStdLibrary(library: File): Boolean {
        return checkAttributeValue(library, TITLE_KOTLIN_JAVASCRIPT_STDLIB, Attributes.Name.IMPLEMENTATION_TITLE)
    }

    private fun getManifestFromJar(library: File): Manifest? {
        if (!library.canRead()) return null

        try {
            JarFile(library).use { jarFile ->
                return jarFile.manifest
            }
        }
        catch (ignored: IOException) {
            return null
        }
    }

    private fun getManifestFromDirectory(library: File): Manifest? {
        if (!library.canRead() || !library.isDirectory) return null

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
            if (library.isDirectory) getManifestFromDirectory(library) else getManifestFromJar(library)

    private fun getManifestMainAttributesFromJarOrDirectory(library: File): Attributes? =
            getManifestFromJarOrDirectory(library)?.mainAttributes

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
