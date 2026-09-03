/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.name

/**
 * Derives target class names for Kotlin scripts and snippets.
 *
 * When a script or a snippet is compiled, its contents are wrapped in a synthetic class. The functions here produce the name of that
 * class from the script file name, or from the special name of the corresponding declaration.
 */
object ScriptNames {
    private val SANITIZE_AS_JAVA_INVALID_CHARACTERS = "[^\\p{L}\\p{Digit}]".toRegex()

    /**
     * Returns the target class name for a script file: the file name without its directories and extension, sanitized and capitalized
     * as a Java class name.
     *
     * E.g. `"pkg/someScript.kts"` -> `SomeScript`, `"1.kts"` -> `_1`.
     */
    @JvmStatic
    fun getScriptNameForFile(filePath: String): Name =
        Name.identifier(capitalizedJavaClassName(filePath.substringAfterLast('/').substringBeforeLast('.')))

    /**
     * Returns the target class name for a script declaration named [originalName].
     *
     * A special name of the form `<script-foo.kts>` is converted with [getScriptNameForFile] (producing `Foo` for this example); a
     * regular name is returned as is.
     */
    @JvmStatic
    fun getScriptTargetClassName(originalName: Name): Name = getSnippetOrScriptTargetClassName(originalName, "script-")

    /**
     * Returns the target class name for a snippet declaration named [originalName].
     *
     * A special name of the form `<snippet-foo.kts>` is converted with [getScriptNameForFile] (producing `Foo` for this example); a
     * regular name is returned as is.
     */
    @JvmStatic
    fun getSnippetTargetClassName(originalName: Name): Name = getSnippetOrScriptTargetClassName(originalName, "snippet-")

    /** Returns the target class name for a snippet compiled from the file named [fileName], e.g. `"foo.kts"` -> `Foo`. */
    @JvmStatic
    fun getSnippetTargetClassName(fileName: String): Name = getSnippetTargetClassName(Name.special("<$fileName>"))

    private fun getSnippetOrScriptTargetClassName(originalName: Name, prefix: String): Name =
        if (originalName.isSpecial) {
            getScriptNameForFile(originalName.asStringStripSpecialMarkers().removePrefix(prefix))
        } else originalName

    /**
     * Capitalizes [shortFileName] and sanitizes it so that it's a valid Java identifier; mirrors
     * `NameUtils.getPackagePartClassNamePrefix`. E.g. "fileName" -> "FileName", "1" -> "_1", "" -> "_"
     */
    private fun capitalizedJavaClassName(shortFileName: String): String {
        if (shortFileName.isEmpty()) return "_"
        val sanitized = SANITIZE_AS_JAVA_INVALID_CHARACTERS.replace(shortFileName, "_")
        // NB `uppercase` uses Locale.ROOT and is locale-independent.
        return if (Character.isJavaIdentifierStart(sanitized[0])) sanitized[0].uppercase() + sanitized.substring(1) else "_$sanitized"
    }
}
