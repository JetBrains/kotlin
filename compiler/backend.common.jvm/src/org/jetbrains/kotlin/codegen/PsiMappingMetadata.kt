/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.org.objectweb.asm.Type

/**
 * One JVM method of a compiled module together with the source range of the declaration it was generated from.
 */
data class PsiMappingEntry(
    val className: String,
    val file: String,
    val packageName: String,
    val name: String,
    val signature: String,
    val startOffset: Int,
    val endOffset: Int,
)

/**
 * Encoding of the PSI <-> bytecode mapping stored in the `mappingParts` argument of `kotlin.internal.PSIMappingMetadata`.
 *
 * With `-Xgenerate-psi-mapping`, every module gets one synthetic class (see [bearerClassInternalName]) annotated with
 * `@PSIMappingMetadata` that describes the JVM methods of all classes of the module.
 *
 * Format (version 2.0): a sequence of text parts, each starting with a `#PSI_MAPPING_<index>` header line.
 * ```
 * #PSI_MAPPING_0
 * 2.0                    // format version
 * <N>                    // size of the string pool
 * <N pool strings, one per line>
 * <entries, one per line>: <class>;<file>;<package>;<name>;<descriptor>;<startOffset>;<endOffset>
 * ```
 * `<class>`, `<file>`, `<package>`, `<name>` and `<descriptor>` are indices into the string pool. Pool strings and entries are
 * split into several parts so that no part exceeds the class file constant length limit; continuation parts contain only the
 * header line followed by further pool strings or entries.
 */
object PsiMappingMetadata {
    const val VERSION = "2.0"
    const val PART_HEADER = "#PSI_MAPPING"
    const val MAPPING_PARTS_ARGUMENT = "mappingParts"

    @JvmField
    val ANNOTATION_TYPE: Type = Type.getObjectType("kotlin/internal/PSIMappingMetadata")

    /** Maximum length in bytes of a modified UTF-8 constant in a class file. */
    private const val UTF8_CONSTANT_MAX_LENGTH = 65535

    fun bearerClassInternalName(moduleName: String): String =
        "PSIMapping$" + moduleName.replace(Regex("[^A-Za-z0-9_]"), "_")

    fun encode(entries: Collection<PsiMappingEntry>): List<String> {
        // The same class may be generated more than once (e.g. a local class of a function with default arguments is generated
        // both from the function and from its `$default` copy), so identical entries are collapsed.
        val sortedEntries = entries.distinct().sortedWith(
            compareBy({ it.className }, { it.name }, { it.signature }, { it.file }, { it.startOffset }, { it.endOffset })
        )

        val pool = LinkedHashMap<String, Int>()
        fun index(string: String): Int = pool.getOrPut(string) { pool.size }

        val encodedEntries = sortedEntries.map { entry ->
            listOf(
                index(entry.className), index(entry.file), index(entry.packageName), index(entry.name), index(entry.signature),
                entry.startOffset, entry.endOffset,
            ).joinToString(";")
        }

        val parts = mutableListOf<StringBuilder>()
        var currentLength = 0

        fun startPart() {
            val header = "${PART_HEADER}_${parts.size}"
            parts += StringBuilder().appendLine(header)
            currentLength = modifiedUtf8Length(header) + 1
        }

        fun appendLine(line: String) {
            val length = modifiedUtf8Length(line) + 1
            if (currentLength + length > UTF8_CONSTANT_MAX_LENGTH) startPart()
            parts.last().appendLine(line)
            currentLength += length
        }

        startPart()
        appendLine(VERSION)
        appendLine(pool.size.toString())
        pool.keys.forEach(::appendLine)
        encodedEntries.forEach(::appendLine)

        return parts.map { it.toString() }
    }

    private fun modifiedUtf8Length(string: String): Int {
        var length = 0
        for (char in string) {
            length += when (char.code) {
                in 1..0x7F -> 1
                in 0..0x7FF -> 2
                else -> 3
            }
        }
        return length
    }
}
