/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.name

object NameUtils {
    private val SANITIZE_AS_JAVA_INVALID_CHARACTERS = "[^\\p{L}\\p{Digit}]".toRegex()

    @JvmStatic
    val CONTEXT_RECEIVER_PREFIX = "\$context_receiver"

    @JvmStatic
    fun sanitizeAsJavaIdentifier(name: String): String {
        return SANITIZE_AS_JAVA_INVALID_CHARACTERS.replace(name, "_")
    }

    /**
     * Capitalizes the short name of the file (without extension) and sanitizes it so that it's a valid Java identifier.
     * E.g. "fileName" -> "FileName", "1" -> "_1", "" -> "_"
     */
    @JvmStatic
    fun getPackagePartClassNamePrefix(shortFileName: String): String =
        if (shortFileName.isEmpty())
            "_"
        else
            capitalizeAsJavaClassName(sanitizeAsJavaIdentifier(shortFileName))

    @JvmStatic
    private fun capitalizeAsJavaClassName(str: String): String =
        // NB `uppercase` uses Locale.ROOT and is locale-independent.
        // See Javadoc on java.lang.String.toUpperCase() for more details.
        if (Character.isJavaIdentifierStart(str[0]))
            str[0].uppercase() + str.substring(1)
        else
            "_$str"

    // "pkg/someScript.kts" -> "SomeScript"
    @JvmStatic
    fun getScriptNameForFile(filePath: String): Name =
        Name.identifier(NameUtils.getPackagePartClassNamePrefix(filePath.substringAfterLast('/').substringBeforeLast('.')))

    @JvmStatic
    fun getScriptTargetClassName(originalName: Name): Name = getSnippetOrScriptTargetClassName(originalName, "script-")

    @JvmStatic
    fun getSnippetTargetClassName(originalName: Name): Name = getSnippetOrScriptTargetClassName(originalName, "snippet-")

    @JvmStatic
    fun getSnippetTargetClassName(fileName: String): Name = getSnippetTargetClassName(Name.special("<$fileName>"))

    private fun getSnippetOrScriptTargetClassName(originalName: Name, prefix: String): Name =
        if (originalName.isSpecial) {
            getScriptNameForFile(originalName.asStringStripSpecialMarkers().removePrefix(prefix))
        } else originalName

    @JvmStatic
    fun hasName(name: Name) = name != SpecialNames.NO_NAME_PROVIDED && name != SpecialNames.ANONYMOUS

    @JvmStatic
    fun delegateFieldName(index: Int): Name {
        return Name.identifier("\$\$delegate_$index")
    }

    @JvmStatic
    fun propertyDelegateName(propertyName: Name): Name {
        return Name.identifier("${propertyName.asString()}\$delegate")
    }

    @JvmStatic
    fun contextReceiverName(index: Int): Name =
        Name.identifier("${CONTEXT_RECEIVER_PREFIX}_$index")
}
