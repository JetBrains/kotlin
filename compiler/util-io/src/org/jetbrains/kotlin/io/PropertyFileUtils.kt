/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.io

import org.jetbrains.kotlin.util.parseSpaceSeparatedArgs
import java.io.StringWriter
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.bufferedReader
import kotlin.io.path.bufferedWriter

/**
 * Reads a properties file by the given [Path].
 */
fun Path.readProperties(): Properties {
    val properties = Properties()
    bufferedReader().use { reader -> properties.load(reader) }
    return properties
}

/**
 * Writes the given [properties] to the file.
 * - No comments are written.
 * - All properties are stored in the alphabetical order by their names.
 */
fun Path.writeProperties(properties: Properties) {
    val rawPropertiesText: String = StringWriter().apply { properties.store(this, null) }.toString()

    val filteredPropertiesText = rawPropertiesText
        .split(System.lineSeparator())
        .filterNot { it.isBlank() || it.startsWith("#") }
        .sorted()
        .joinToString("\n", postfix = "\n")

    bufferedWriter().use { writer -> writer.write(filteredPropertiesText) }
}

/**
 * TODO: this method working with suffixes should be replaced with
 *  functionality borrowed from def file parser and unified for interop tool
 *  and kotlin compiler.
 */
fun Properties.propertyList(key: String, suffix: String? = null, escapeInQuotes: Boolean = false): List<String> {
    val value: String? = (getProperty(key.suffix(suffix)) ?: getProperty(key))?.trim(Char::isWhitespace)
    return when {
        value.isNullOrEmpty() -> emptyList()
        escapeInQuotes -> parseSpaceSeparatedArgs(value)
        else -> value.split(Regex("\\s+"))
    }
}

/**
 * Wraps [propertyList] with resolving mechanism. See [String.resolveValue].
 */
fun Properties.resolvablePropertyList(
    key: String, suffix: String? = null, escapeInQuotes: Boolean = false,
    visitedProperties: MutableSet<String> = mutableSetOf()
): List<String> = propertyList(key, suffix, escapeInQuotes).flatMap {
    // We need to create a copy of a visitedProperties to avoid collisions
    // between different elements of the list.
    it.resolveValue(this, visitedProperties.toMutableSet())
}

/**
 * Wraps [propertyString] with resolving mechanism. See [String.resolveValue].
 */
fun Properties.resolvablePropertyString(
    key: String, suffix: String? = null,
    visitedProperties: MutableSet<String> = mutableSetOf()
): String? = (propertyString(key, suffix) ?: findValueByResolvableKey(key.suffix(suffix)))
    ?.split(' ')
    ?.flatMap { it.resolveValue(this, visitedProperties) }
    ?.joinToString(" ")

private fun Properties.findValueByResolvableKey(targetKey: String): String? =
    propertyNames().asSequence()
        .filterIsInstance<String>()
        .filter { it.contains('$') }
        .firstOrNull { propKey -> propKey.resolveValue(this).singleOrNull() == targetKey }
        ?.let { getProperty(it) }

/**
 * Adds trivial symbol resolving mechanism to properties files.
 *
 * Given the following properties file:
 *
 *      key0 = value1 value2
 *      key1 = value3 $key0
 *      key2 = $key1
 *
 * "$key1".resolveValue(properties) will return List("value3", "value1", "value2")
 */
private fun String.resolveValue(properties: Properties, visitedProperties: MutableSet<String> = mutableSetOf()): List<String> =
    when {
        contains("$") -> {
            val sigil = indexOf('$')
            val prefix = substring(0, sigil)
            val afterSigil = substring(sigil + 1)

            val (property, rest) = if (afterSigil.startsWith('{')) {
                val endBrace = afterSigil.indexOf('}')
                require(endBrace != -1) { "Unclosed '{' in: $this" }
                Pair(afterSigil.substring(1, endBrace), afterSigil.substring(endBrace + 1))
            } else {
                Pair(
                    afterSigil.substringBefore('/'),
                    if (afterSigil.contains('/')) "/${afterSigil.substringAfter('/')}" else ""
                )
            }

            val relative = if (rest.startsWith('/')) rest.drop(1) else ""
            val plainSuffix = if (!rest.startsWith('/')) rest else ""

            // Keep track of visited properties to avoid running in circles.
            if (!visitedProperties.add(property)) {
                error("Circular dependency: ${visitedProperties.joinToString()}")
            }
            val substitutionResult = properties.resolvablePropertyList(property, visitedProperties = visitedProperties)
            when {
                substitutionResult.size > 1 -> when {
                    relative.isNotEmpty() ->
                        error("Cannot append `/$relative` to multiple values: ${substitutionResult.joinToString()}")
                    prefix.isNotEmpty() ->
                        error("Cannot add prefix `$prefix` to multiple values: ${substitutionResult.joinToString()}")
                    else -> substitutionResult
                }
                else -> substitutionResult.map {
                    // Avoid repeated '/' at the end.
                    if (relative.isNotEmpty()) {
                        "$prefix${it.dropLastWhile { it == '/' }}/$relative$plainSuffix"
                    } else {
                        "$prefix$it$plainSuffix"
                    }
                }
            }
        }
        else -> listOf(this)
    }

private fun Properties.propertyString(key: String, suffix: String? = null): String? = getProperty(key.suffix(suffix)) ?: this.getProperty(key)

fun String.suffix(suf: String?): String =
    if (suf == null) this
    else "${this}.$suf"
