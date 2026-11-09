/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java

/**
 * Names of JVM parameters which hold the underlying value of an inline class.
 *
 * Such a parameter is named `$v$c$<encoded fq name of the inline class>$$<original parameter name>`. The debugger
 * recognizes the prefix and renders the slot back as an instance of the inline class, and the name cannot clash with
 * any user-declared variable, which the previously used `arg0` did (KT-73995).
 *
 * The contract is: a slot with such a name always holds the underlying value of the inline class, never a boxed
 * instance of it (although the underlying value may in turn be a boxed instance of another inline class).
 * The slot may hold `null` in exactly one of two mutually exclusive situations, which can be told apart by looking at
 * the underlying type of the inline class:
 * - the underlying type is a non-null reference type: the declared parameter type was nullable, and the instance
 *   itself is `null`;
 * - the underlying type is nullable: the declared parameter type was non-null (a nullable one would have been boxed),
 *   and the instance wraps `null`.
 *
 * The encoded fq name consists of Java identifier characters only, so that the whole name stays a valid Java identifier
 * whenever the original parameter name is one. This matters because IntelliJ's class file stub builder discards
 * parameter names which are not valid Java identifiers, which would make the encoded names invisible to all
 * cls-based tooling. `$` is the escape character:
 * - `$_` stands for `.`, `$S` for a literal `$`;
 * - `$u` followed by four hex digits stands for any other BMP character which is not a Java identifier part
 *   (`-`, space, etc.), `$U` followed by eight hex digits for a supplementary one;
 * - any other character is kept as is.
 * Since `$` inside the encoded fq name is always followed by one of `_`, `S`, `u`, `U`, the sequence `$$` never occurs
 * there and unambiguously separates the fq name from the parameter name, which follows verbatim.
 *
 * This lives next to [JvmAbi] so that every consumer of bytecode parameter names (the JVM backend which produces
 * them, kapt stubs and the IDE which show them to the user) shares one definition.
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

    private fun String.hexAt(start: Int, digits: Int): Int? {
        if (start + digits > length) return null
        return substring(start, start + digits).toIntOrNull(16)
    }

    /** Returns the original parameter name if [name] was produced by [encode], or [name] itself otherwise. */
    fun originalParameterName(name: String): String = decodeValueClassParameterNameOrNull(name)?.parameterName ?: name
}
