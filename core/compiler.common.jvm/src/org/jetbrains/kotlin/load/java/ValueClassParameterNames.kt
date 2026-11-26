/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java

/**
 * Names of JVM parameters and local variables that hold the underlying value of an inline class (KT-73995):
 * `$v$c$<encoded fq name of the inline class>$$<source name>`. Such a slot never holds a boxed instance of the inline class.
 * The encoded fq name has only Java identifier characters, because IntelliJ class file stubs drop other parameter names.
 *
 * The IntelliJ debugger reads these names in `decodeValueClassVariableName`, see intellij-community
 * `plugins/kotlin/jvm-debugger/base/util/src/org/jetbrains/kotlin/idea/debugger/base/util/valueClassVariableNames.kt`.
 */
object ValueClassParameterNames {
    private const val PREFIX = "\$v\$c\$"
    private const val SEPARATOR = "\$\$"

    private fun StringBuilder.appendEncodedClassName(classFqName: String) {
        var i = 0
        while (i < classFqName.length) {
            val cp = classFqName.codePointAt(i)
            when {
                cp == '.'.code -> append("\$_")
                cp == '$'.code -> append("\$S")
                Character.isJavaIdentifierPart(cp) && !Character.isIdentifierIgnorable(cp) -> appendCodePoint(cp)
                cp <= 0xFFFF -> append("\$u").append(String.format("%04x", cp))
                else -> append("\$U").append(String.format("%08x", cp))
            }
            i += Character.charCount(cp)
        }
    }

    /**
     * Encodes [parameterName] as the name of a parameter holding the underlying value of the inline class [classFqName].
     * [classFqName] may be empty if the class has no fq name.
     */
    fun encode(classFqName: String, parameterName: String): String = buildString {
        append(PREFIX)
        appendEncodedClassName(classFqName)
        append(SEPARATOR)
        append(parameterName)
    }

    /**
     * The result of decoding a name produced by [encode].
     *
     * [classFqName] is the fq name of the inline class as a string (empty if the class had no fq name),
     * [parameterName] is the original name of the parameter, verbatim.
     */
    class Decoded(val classFqName: String, val parameterName: String)

    /**
     * Decodes a name produced by [encode], or returns `null` if [name] is not such a name.
     */
    fun decodeValueClassParameterNameOrNull(name: String): Decoded? {
        if (!name.startsWith(PREFIX)) return null
        val classFqName = StringBuilder()
        var i = PREFIX.length
        while (i < name.length) {
            val c = name[i]
            if (c != '$') {
                classFqName.append(c)
                i++
                continue
            }
            when (name.getOrNull(i + 1)) {
                '$' -> return Decoded(classFqName.toString(), name.substring(i + 2))
                '_' -> { classFqName.append('.'); i += 2 }
                'S' -> { classFqName.append('$'); i += 2 }
                'u' -> { classFqName.appendCodePoint(name.hexAt(i + 2, 4) ?: return null); i += 6 }
                'U' -> { classFqName.appendCodePoint(name.hexAt(i + 2, 8) ?: return null); i += 10 }
                else -> return null
            }
        }
        return null
    }

    /**
     * Reads [digits] hex digits at [start] as a code point.
     * Returns `null` if they are not hex digits or not a valid code point.
     */
    private fun String.hexAt(start: Int, digits: Int): Int? {
        if (start + digits > length) return null
        var codePoint = 0
        for (i in start until start + digits) {
            val digit = Character.digit(this[i], 16)
            if (digit < 0) return null
            codePoint = codePoint * 16 + digit
        }
        return codePoint.takeIf { Character.isValidCodePoint(it) }
    }

    /**
     * Returns the index of the original parameter name in [name] if [name] was produced by [encode], or 0 otherwise.
     * A suffix that the compiler adds to the name, for example for an inlined variable, starts at this index or later.
     */
    fun sourceNameStart(name: String): Int = decodeValueClassParameterNameOrNull(name)?.let { name.length - it.parameterName.length } ?: 0

    /** Returns the original parameter name if [name] was produced by [encode], or [name] itself otherwise. */
    fun originalParameterName(name: String): String = decodeValueClassParameterNameOrNull(name)?.parameterName ?: name
}
