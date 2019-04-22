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

package org.jetbrains.kotlin.modules

import org.jetbrains.kotlin.build.JvmSourceRoot
import org.jetbrains.kotlin.cli.common.modules.ModuleXmlParser.*
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.utils.Printer
import java.io.File

class KotlinModuleXmlBuilder {
    private val xml = StringBuilder()
    private val p = Printer(xml)
    private var done = false

    init {
        openTag(p, MODULES)
    }

    fun addModule(
        moduleName: String,
        outputDir: String,
        sourceFiles: Iterable<File>,
        javaSourceRoots: Iterable<JvmSourceRoot>,
        classpathRoots: Iterable<File>,
        commonSourceFiles: Iterable<File>,
        modularJdkRoot: File?,
        targetTypeId: String,
        isTests: Boolean,
        directoriesToFilterOut: Set<File>,
        friendDirs: Iterable<File>
    ): KotlinModuleXmlBuilder {
        assert(!done) { "Already done" }

        p.println("<!-- Module script for ${if (isTests) "tests" else "production"} -->")

        p.println("<", MODULE, " ",
                  NAME, "=\"", escapeXml(moduleName), "\" ",
                  TYPE, "=\"", escapeXml(targetTypeId), "\" ",
                  OUTPUT_DIR, "=\"", getEscapedPath(File(outputDir)), "\">")
        p.pushIndent()

        for (friendDir in friendDirs) {
            p.println("<", FRIEND_DIR, " ", PATH, "=\"", getEscapedPath(friendDir), "\"/>")
        }

        for (sourceFile in sourceFiles) {
            p.println("<", SOURCES, " ", PATH, "=\"", getEscapedPath(sourceFile), "\"/>")
        }

        for (commonSourceFile in commonSourceFiles) {
            p.println("<", COMMON_SOURCES, " ", PATH, "=\"", getEscapedPath(commonSourceFile), "\"/>")
        }

        processJavaSourceRoots(javaSourceRoots)
        processClasspath(classpathRoots, directoriesToFilterOut)

        if (modularJdkRoot != null) {
            p.println("<", MODULAR_JDK_ROOT, " ", PATH, "=\"", getEscapedPath(modularJdkRoot), "\"/>")
        }

        closeTag(p, MODULE)
        return this
    }

    private fun processClasspath(
            files: Iterable<File>,
            directoriesToFilterOut: Set<File>) {
        p.println("<!-- Classpath -->")
        for (file in files) {
            val isOutput = directoriesToFilterOut.contains(file) && !IncrementalCompilation.isEnabledForJvm()
            if (isOutput) {
                // For IDEA's make (incremental compilation) purposes, output directories of the current module and its dependencies
                // appear on the class path, so we are at risk of seeing the results of the previous build, i.e. if some class was
                // removed in the sources, it may still be there in binaries. Thus, we delete these entries from the classpath.
                p.println("<!-- Output directory, commented out -->")
                p.println("<!-- ")
                p.pushIndent()
            }

            p.println("<", CLASSPATH, " ", PATH, "=\"", getEscapedPath(file), "\"/>")

            if (isOutput) {
                p.popIndent()
                p.println("-->")
            }
        }
    }

    private fun processJavaSourceRoots(roots: Iterable<JvmSourceRoot>) {
        p.println("<!-- Java source roots -->")
        for (root in roots) {
            p.print("<")
            p.printWithNoIndent(JAVA_SOURCE_ROOTS, " ", PATH, "=\"", getEscapedPath(root.file), "\"")

            if (root.packagePrefix != null) {
                p.printWithNoIndent(" ", JAVA_SOURCE_PACKAGE_PREFIX, "=\"", root.packagePrefix, "\"")
            }

            p.printWithNoIndent("/>")
            p.println()
        }
    }

    fun asText(): CharSequence {
        if (!done) {
            closeTag(p, MODULES)
            done = true
        }
        return xml
    }

    private fun openTag(p: Printer, tag: String) {
        p.println("<$tag>")
        p.pushIndent()
    }

    private fun closeTag(p: Printer, tag: String) {
        p.popIndent()
        p.println("</$tag>")
    }

    private fun getEscapedPath(sourceFile: File): String {
        return escapeXml(sourceFile.invariantSeparatorsPath)
    }

    private companion object {
        private val xmlEscapeReplacement = mapOf("<" to "&lt;", ">" to "&gt;", "&" to "&amp;", "'" to "&#39;", "\"" to "&quot;")
        private val xmlEscapeRegex = Regex(xmlEscapeReplacement.keys.joinToString("|", "(?:", ")") { Regex.escape(it) })

        private fun escapeXml(string: String) = string.replace(xmlEscapeRegex) { xmlEscapeReplacement.getValue(it.value) }
    }
}
