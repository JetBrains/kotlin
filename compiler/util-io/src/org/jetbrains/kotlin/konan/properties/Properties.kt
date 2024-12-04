/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.properties

import org.jetbrains.kotlin.konan.file.*
import org.jetbrains.kotlin.util.parseSpaceSeparatedArgs
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.StringWriter

typealias Properties = java.util.Properties

fun File.loadProperties(): Properties {
    val properties = java.util.Properties()
    this.bufferedReader().use { reader ->
        properties.load(reader)
    }
    return properties
}

fun loadProperties(path: String): Properties = File(path).loadProperties()

/**
 * Standard properties writer has two issues, which prevents build reproducibility
 *
 * 1. The order of lines is not defined
 * 2. It uses platform-specific end-of-lines
 *
 * This function deals with both issues
 */
fun File.saveProperties(properties: Properties) {
    val rawData = StringWriter().apply {
        properties.store(this, null)
    }.toString()

    val lines = rawData
        .split(System.lineSeparator())
        .filterNot { it.isEmpty() || it.startsWith("#") }
        .sorted()

    outputStream().use {
        it.write(lines.joinToString("\n", postfix = "\n").toByteArray())
    }
}

fun Properties.saveToFile(file: File) = file.saveProperties(this)

fun Properties.propertyString(key: String, suffix: String? = null): String? = getProperty(key.suffix(suffix)) ?: this.getProperty(key)

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

fun Properties.hasProperty(key: String, suffix: String? = null): Boolean
        = this.getProperty(key.suffix(suffix)) != null

fun String.suffix(suf: String?): String =
    if (suf == null) this
    else "${this}.$suf"

fun Properties.keepOnlyDefaultProfiles() {
    val DEPENDENCY_PROFILES_KEY = "dependencyProfiles"
    val dependencyProfiles = this.getProperty(DEPENDENCY_PROFILES_KEY)
    if (dependencyProfiles != "default alt")
        error("unexpected $DEPENDENCY_PROFILES_KEY value: expected 'default alt', got '$dependencyProfiles'")

    // Force build to use only 'default' profile:
    this.setProperty(DEPENDENCY_PROFILES_KEY, "default")
    // TODO: it actually affects only resolution made in :dependencies,
    // that's why we assume that 'default' profile comes first (and check this above).
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
): String? = propertyString(key, suffix)
    ?.split(' ')
    ?.flatMap { it.resolveValue(this, visitedProperties) }
    ?.joinToString(" ")

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
            val prefix = this.substringBefore('$', missingDelimiterValue = "")
            val withoutSigil = this.substringAfter('$')
            val property = withoutSigil.substringBefore('/')
            val relative = withoutSigil.substringAfter('/', missingDelimiterValue = "")
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
                        "$prefix${it.dropLastWhile { it == '/' }}/$relative"
                    } else {
                        "$prefix$it"
                    }
                }
            }
        }
        else -> listOf(this)
    }
