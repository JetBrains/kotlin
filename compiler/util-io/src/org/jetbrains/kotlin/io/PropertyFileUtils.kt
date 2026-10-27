/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.io

import org.jetbrains.kotlin.util.parseSpaceSeparatedArgs
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintStream
import java.io.PrintWriter
import java.io.Reader
import java.io.StringWriter
import java.io.Writer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Arrays
import java.util.Enumeration
import java.util.Properties
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Function
import kotlin.io.path.bufferedWriter

private const val DEPENDS_KEY: String = "depends"
private const val BYTE_TAB: Byte = '\t'.code.toByte()
private const val BYTE_LF: Byte = '\n'.code.toByte()
private const val BYTE_FF: Byte = '\u000C'.code.toByte()
private const val BYTE_CR: Byte = '\r'.code.toByte()
private const val BYTE_SPACE: Byte = ' '.code.toByte()
private const val BYTE_BANG: Byte = '!'.code.toByte()
private const val BYTE_HASH: Byte = '#'.code.toByte()
private const val BYTE_DOT: Byte = '.'.code.toByte()
private const val BYTE_COLON: Byte = ':'.code.toByte()
private const val BYTE_EQUALS: Byte = '='.code.toByte()
private const val BYTE_BACKSLASH: Byte = '\\'.code.toByte()
private const val BYTE_LOWER_D: Byte = 'd'.code.toByte()
private const val BYTE_LOWER_E: Byte = 'e'.code.toByte()
private const val BYTE_LOWER_N: Byte = 'n'.code.toByte()
private const val BYTE_LOWER_P: Byte = 'p'.code.toByte()
private const val BYTE_LOWER_S: Byte = 's'.code.toByte()
private const val UTF8_BOM_0: Byte = 0xEF.toByte()
private const val UTF8_BOM_1: Byte = 0xBB.toByte()
private const val UTF8_BOM_2: Byte = 0xBF.toByte()

/**
 * [Properties] subclass returned by [createPropertiesFromBytes] and [Path.readProperties]
 * that records whether any property key contains `'.'` and defers `depends=` string decoding
 * until `"depends"` or a bulk/enumeration/mutation operation is accessed.
 */
class ManifestProperties(hasDottedKey: Boolean = false) : Properties() {
    var hasDottedKey: Boolean = hasDottedKey
        private set

    @Volatile
    private var pendingDependsBytes: ByteArray? = null

    @Volatile
    private var pendingDependsHasNonAscii: Boolean = false

    val hasDeferredDepends: Boolean
        get() = pendingDependsBytes != null

    internal fun markDottedKey() {
        hasDottedKey = true
    }

    internal fun putParsed(key: String, value: String) {
        if (key == DEPENDS_KEY && pendingDependsBytes != null) {
            synchronized(this) {
                pendingDependsBytes = null
                super.put(key, value)
            }
            return
        }
        super.put(key, value)
    }

    internal fun setPendingDepends(slice: ByteArray, hasNonAscii: Boolean) {
        synchronized(this) {
            if (super.containsKey(DEPENDS_KEY)) {
                super.remove(DEPENDS_KEY)
            }
            pendingDependsHasNonAscii = hasNonAscii
            pendingDependsBytes = slice
        }
    }

    private fun materializeDepends() {
        if (pendingDependsBytes == null) return
        synchronized(this) {
            val slice = pendingDependsBytes ?: return
            val decoded = decodeBytesSlice(slice, 0, slice.size, pendingDependsHasNonAscii)
            super.put(DEPENDS_KEY, decoded)
            pendingDependsBytes = null
        }
    }

    private fun checkKeyForDot(key: Any?) {
        if (!hasDottedKey && key is String && key.indexOf('.') >= 0) {
            hasDottedKey = true
        }
    }

    private fun scanAllKeysForDot() {
        if (!hasDottedKey) {
            for (key in super.keys) {
                if (key is String && key.indexOf('.') >= 0) {
                    hasDottedKey = true
                    break
                }
            }
        }
    }

    override fun getProperty(key: String?): String? {
        if (key == DEPENDS_KEY) {
            materializeDepends()
        }
        return super.getProperty(key)
    }

    override fun getProperty(key: String?, defaultValue: String?): String? {
        if (key == DEPENDS_KEY) {
            materializeDepends()
        }
        return super.getProperty(key, defaultValue)
    }

    @Synchronized
    override fun get(key: Any?): Any? {
        if (key == DEPENDS_KEY) {
            materializeDepends()
        }
        return super.get(key)
    }

    @Synchronized
    override fun getOrDefault(key: Any?, defaultValue: Any?): Any? {
        if (key == DEPENDS_KEY) {
            materializeDepends()
        }
        return super.getOrDefault(key, defaultValue)
    }

    @Synchronized
    override fun containsKey(key: Any?): Boolean {
        if (key == DEPENDS_KEY) {
            materializeDepends()
        }
        return super.containsKey(key)
    }

    override fun stringPropertyNames(): MutableSet<String> {
        materializeDepends()
        return super.stringPropertyNames()
    }

    override fun propertyNames(): Enumeration<*> {
        materializeDepends()
        return super.propertyNames()
    }

    @Synchronized
    override fun keys(): Enumeration<Any> {
        materializeDepends()
        return super.keys()
    }

    @Synchronized
    override fun elements(): Enumeration<Any> {
        materializeDepends()
        return super.elements()
    }

    override val keys: MutableSet<Any>
        get() {
            materializeDepends()
            return super.keys
        }

    override val entries: MutableSet<MutableMap.MutableEntry<Any, Any>>
        get() {
            materializeDepends()
            return super.entries
        }

    override val values: MutableCollection<Any>
        get() {
            materializeDepends()
            return super.values
        }

    override val size: Int
        @Synchronized
        get() {
            materializeDepends()
            return super.size
        }

    @Synchronized
    override fun isEmpty(): Boolean {
        materializeDepends()
        return super.isEmpty()
    }

    @Synchronized
    override fun contains(value: Any?): Boolean {
        materializeDepends()
        return super.contains(value)
    }

    @Synchronized
    override fun containsValue(value: Any?): Boolean {
        materializeDepends()
        return super.containsValue(value)
    }

    @Synchronized
    override fun forEach(action: BiConsumer<in Any, in Any>) {
        materializeDepends()
        super.forEach(action)
    }

    @Synchronized
    override fun replaceAll(function: BiFunction<in Any, in Any, out Any>) {
        materializeDepends()
        super.replaceAll(function)
    }

    override fun store(writer: Writer?, comments: String?) {
        materializeDepends()
        super.store(writer, comments)
    }

    override fun store(out: OutputStream?, comments: String?) {
        materializeDepends()
        super.store(out, comments)
    }

    @Deprecated("Deprecated in java.util.Properties")
    override fun save(out: OutputStream?, comments: String?) {
        materializeDepends()
        @Suppress("DEPRECATION")
        super.save(out, comments)
    }

    override fun storeToXML(os: OutputStream?, comment: String?) {
        materializeDepends()
        super.storeToXML(os, comment)
    }

    override fun storeToXML(os: OutputStream?, comment: String?, encoding: String?) {
        materializeDepends()
        super.storeToXML(os, comment, encoding)
    }

    override fun list(out: PrintStream?) {
        materializeDepends()
        super.list(out)
    }

    override fun list(out: PrintWriter?) {
        materializeDepends()
        super.list(out)
    }

    @Synchronized
    override fun put(key: Any?, value: Any?): Any? {
        materializeDepends()
        checkKeyForDot(key)
        return super.put(key, value)
    }

    @Synchronized
    override fun setProperty(key: String?, value: String?): Any? {
        materializeDepends()
        checkKeyForDot(key)
        return super.setProperty(key, value)
    }

    @Synchronized
    override fun putIfAbsent(key: Any?, value: Any?): Any? {
        materializeDepends()
        checkKeyForDot(key)
        return super.putIfAbsent(key, value)
    }

    @Synchronized
    override fun putAll(t: Map<out Any?, Any?>) {
        materializeDepends()
        if (!hasDottedKey) {
            for (key in t.keys) {
                if (key is String && key.indexOf('.') >= 0) {
                    hasDottedKey = true
                    break
                }
            }
        }
        super.putAll(t)
    }

    @Synchronized
    override fun remove(key: Any?): Any? {
        materializeDepends()
        return super.remove(key)
    }

    @Synchronized
    override fun remove(key: Any?, value: Any?): Boolean {
        materializeDepends()
        return super.remove(key, value)
    }

    @Synchronized
    override fun replace(key: Any?, value: Any?): Any? {
        materializeDepends()
        return super.replace(key, value)
    }

    @Synchronized
    override fun replace(key: Any?, oldValue: Any?, newValue: Any?): Boolean {
        materializeDepends()
        return super.replace(key, oldValue, newValue)
    }

    @Synchronized
    override fun computeIfAbsent(key: Any?, mappingFunction: Function<in Any?, out Any?>): Any? {
        materializeDepends()
        val result = super.computeIfAbsent(key, mappingFunction)
        if (result != null) {
            checkKeyForDot(key)
        }
        return result
    }

    @Synchronized
    override fun computeIfPresent(key: Any?, remappingFunction: BiFunction<in Any?, in Any, out Any?>): Any? {
        materializeDepends()
        return super.computeIfPresent(key, remappingFunction)
    }

    @Synchronized
    override fun compute(key: Any?, remappingFunction: BiFunction<in Any?, in Any?, out Any?>): Any? {
        materializeDepends()
        val result = super.compute(key, remappingFunction)
        if (result != null) {
            checkKeyForDot(key)
        }
        return result
    }

    @Synchronized
    override fun merge(key: Any?, value: Any, remappingFunction: BiFunction<in Any, in Any, out Any?>): Any? {
        materializeDepends()
        val result = super.merge(key, value, remappingFunction)
        if (result != null) {
            checkKeyForDot(key)
        }
        return result
    }

    @Synchronized
    override fun clear() {
        pendingDependsBytes = null
        super.clear()
    }

    @Synchronized
    override fun load(reader: Reader?) {
        materializeDepends()
        super.load(reader)
        scanAllKeysForDot()
    }

    @Synchronized
    override fun load(inStream: InputStream?) {
        materializeDepends()
        super.load(inStream)
        scanAllKeysForDot()
    }

    @Synchronized
    override fun loadFromXML(`in`: InputStream?) {
        materializeDepends()
        super.loadFromXML(`in`)
        scanAllKeysForDot()
    }

    @Synchronized
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        materializeDepends()
        if (other is ManifestProperties) {
            other.materializeDepends()
        }
        return super.equals(other)
    }

    @Synchronized
    override fun hashCode(): Int {
        materializeDepends()
        return super.hashCode()
    }

    @Synchronized
    override fun toString(): String {
        materializeDepends()
        return super.toString()
    }

    @Synchronized
    override fun clone(): Any {
        materializeDepends()
        return super.clone()
    }
}

/**
 * Parses a `.properties` payload directly from a UTF-8 [ByteArray] without allocating
 * intermediate stream/reader buffers.
 */
fun createPropertiesFromBytes(bytes: ByteArray): Properties {
    val properties = ManifestProperties()
    val size = bytes.size
    var i = 0
    if (size >= 3 && bytes[0] == UTF8_BOM_0 && bytes[1] == UTF8_BOM_1 && bytes[2] == UTF8_BOM_2) {
        i = 3
    }

    while (i < size) {
        // Skip leading horizontal whitespace and blank lines.
        while (i < size) {
            val b = bytes[i]
            if (b == BYTE_SPACE || b == BYTE_TAB || b == BYTE_FF || b == BYTE_LF || b == BYTE_CR) {
                i++
            } else {
                break
            }
        }
        if (i >= size) break

        val first = bytes[i]
        // Comment lines start with '#' or '!' and never continue across lines on trailing '\'.
        if (first == BYTE_HASH || first == BYTE_BANG) {
            i++
            while (i < size) {
                val b = bytes[i]
                if (b == BYTE_LF || b == BYTE_CR) {
                    i++
                    break
                }
                i++
            }
            continue
        }

        val lineStart = i
        var p = lineStart
        var keyHasNonAscii = false
        var keyHasDot = false
        var hasSep = false
        var hitBackslash = false

        while (p < size) {
            val b = bytes[p]
            if (b == BYTE_BACKSLASH) {
                hitBackslash = true
                break
            }
            if (b == BYTE_LF || b == BYTE_CR) {
                break
            }
            if (b == BYTE_EQUALS || b == BYTE_COLON) {
                hasSep = true
                break
            }
            if (b == BYTE_SPACE || b == BYTE_TAB || b == BYTE_FF) {
                break
            }
            if (b < 0) {
                keyHasNonAscii = true
            } else if (b == BYTE_DOT) {
                keyHasDot = true
            }
            p++
        }

        if (hitBackslash) {
            i = parseEscapedLogicalLine(bytes, lineStart, properties)
            continue
        }

        val keyEnd = p
        if (p < size) {
            val sepByte = bytes[p]
            if (sepByte == BYTE_EQUALS || sepByte == BYTE_COLON || sepByte == BYTE_SPACE || sepByte == BYTE_TAB || sepByte == BYTE_FF) {
                p++
            }
        }

        while (p < size) {
            val b = bytes[p]
            if (b != BYTE_SPACE && b != BYTE_TAB && b != BYTE_FF) {
                if (!hasSep && (b == BYTE_EQUALS || b == BYTE_COLON)) {
                    hasSep = true
                } else {
                    break
                }
            }
            p++
        }

        val valStart = p
        var valHasNonAscii = false
        while (p < size) {
            val b = bytes[p]
            if (b == BYTE_BACKSLASH) {
                hitBackslash = true
                break
            }
            if (b == BYTE_LF || b == BYTE_CR) {
                break
            }
            if (b < 0) {
                valHasNonAscii = true
            }
            p++
        }

        if (hitBackslash) {
            i = parseEscapedLogicalLine(bytes, lineStart, properties)
            continue
        }

        val valEnd = p
        i = consumeLineBreak(bytes, p)

        val keyLen = keyEnd - lineStart
        if (isDependsKey(bytes, lineStart, keyLen)) {
            properties.setPendingDepends(Arrays.copyOfRange(bytes, valStart, valEnd), valHasNonAscii)
        } else {
            val key = decodeBytesSlice(bytes, lineStart, keyLen, keyHasNonAscii)
            if (keyHasDot) {
                properties.markDottedKey()
            }
            val value = decodeBytesSlice(bytes, valStart, valEnd - valStart, valHasNonAscii)
            properties.putParsed(key, value)
        }
    }

    return properties
}

private fun isDependsKey(bytes: ByteArray, offset: Int, length: Int): Boolean =
    length == 7 &&
        bytes[offset] == BYTE_LOWER_D &&
        bytes[offset + 1] == BYTE_LOWER_E &&
        bytes[offset + 2] == BYTE_LOWER_P &&
        bytes[offset + 3] == BYTE_LOWER_E &&
        bytes[offset + 4] == BYTE_LOWER_N &&
        bytes[offset + 5] == BYTE_LOWER_D &&
        bytes[offset + 6] == BYTE_LOWER_S

private fun decodeBytesSlice(bytes: ByteArray, offset: Int, length: Int, hasNonAscii: Boolean): String {
    if (length == 0) return ""
    val charset = if (hasNonAscii) StandardCharsets.UTF_8 else StandardCharsets.ISO_8859_1
    return String(bytes, offset, length, charset)
}

private fun consumeLineBreak(bytes: ByteArray, pos: Int): Int {
    val size = bytes.size
    if (pos >= size) return size
    return if (bytes[pos] == BYTE_CR && pos + 1 < size && bytes[pos + 1] == BYTE_LF) {
        pos + 2
    } else {
        pos + 1
    }
}

private fun parseEscapedLogicalLine(bytes: ByteArray, lineStart: Int, properties: ManifestProperties): Int {
    val size = bytes.size
    val logicalLine = StringBuilder()
    var segStart = lineStart

    while (true) {
        var q = segStart
        var segHasNonAscii = false
        var trailingBackslashes = 0
        while (q < size) {
            val b = bytes[q]
            if (b == BYTE_LF || b == BYTE_CR) {
                break
            }
            if (b == BYTE_BACKSLASH) {
                trailingBackslashes++
            } else {
                trailingBackslashes = 0
                if (b < 0) {
                    segHasNonAscii = true
                }
            }
            q++
        }

        val isOddBackslash = (trailingBackslashes and 1) != 0
        if (isOddBackslash) {
            val contentLen = (q - 1) - segStart
            if (contentLen > 0) {
                logicalLine.append(decodeBytesSlice(bytes, segStart, contentLen, segHasNonAscii))
            }
            if (q >= size) {
                // Trailing odd backslash at EOF without a line terminator: Properties$LineReader
                // strips the backslash and still processes the logical line (even if empty).
                segStart = size
                break
            }
            q = consumeLineBreak(bytes, q)
            while (q < size) {
                val nb = bytes[q]
                if (nb != BYTE_SPACE && nb != BYTE_TAB && nb != BYTE_FF) {
                    break
                }
                q++
            }
            if (q >= size) {
                segStart = size
                break
            }
            val nextByte = bytes[q]
            if (nextByte == BYTE_LF || nextByte == BYTE_CR) {
                q = consumeLineBreak(bytes, q)
                if (logicalLine.isEmpty()) {
                    return q
                }
                segStart = q
                break
            }
            segStart = q
        } else {
            val contentLen = q - segStart
            if (contentLen > 0) {
                logicalLine.append(decodeBytesSlice(bytes, segStart, contentLen, segHasNonAscii))
            }
            segStart = consumeLineBreak(bytes, q)
            break
        }
    }

    val limit = logicalLine.length
    var keyLen = 0
    var valueStart = limit
    var hasSep = false
    var precedingBackslash = false
    while (keyLen < limit) {
        val c = logicalLine[keyLen]
        if ((c == '=' || c == ':') && !precedingBackslash) {
            valueStart = keyLen + 1
            hasSep = true
            break
        } else if ((c == ' ' || c == '\t' || c == '\u000C') && !precedingBackslash) {
            valueStart = keyLen + 1
            break
        }
        precedingBackslash = if (c == '\\') !precedingBackslash else false
        keyLen++
    }

    while (valueStart < limit) {
        val c = logicalLine[valueStart]
        if (c != ' ' && c != '\t' && c != '\u000C') {
            if (!hasSep && (c == '=' || c == ':')) {
                hasSep = true
            } else {
                break
            }
        }
        valueStart++
    }

    val key = decodePropertyEscapes(logicalLine, 0, keyLen)
    if (key.indexOf('.') >= 0) {
        properties.markDottedKey()
    }
    val value = decodePropertyEscapes(logicalLine, valueStart, limit - valueStart)
    properties.putParsed(key, value)
    return segStart
}

private fun decodePropertyEscapes(chars: CharSequence, offset: Int, length: Int): String {
    val end = offset + length
    var idx = offset
    while (idx < end && chars[idx] != '\\') {
        idx++
    }
    if (idx == end) {
        return chars.substring(offset, end)
    }

    val out = StringBuilder(length)
    out.append(chars, offset, idx)
    while (idx < end) {
        val ch = chars[idx++]
        if (ch == '\\') {
            if (idx >= end) {
                out.append('\\')
                break
            }
            when (val esc = chars[idx++]) {
                'u' -> {
                    if (idx + 4 > end) {
                        throw IllegalArgumentException("Malformed \\uxxxx encoding.")
                    }
                    var codePoint = 0
                    for (digitIdx in 0 until 4) {
                        val hex = chars[idx++]
                        val digit = when (hex) {
                            in '0'..'9' -> hex - '0'
                            in 'a'..'f' -> 10 + (hex - 'a')
                            in 'A'..'F' -> 10 + (hex - 'A')
                            else -> throw IllegalArgumentException("Malformed \\uxxxx encoding.")
                        }
                        codePoint = (codePoint shl 4) + digit
                    }
                    out.append(codePoint.toChar())
                }
                't' -> out.append('\t')
                'r' -> out.append('\r')
                'n' -> out.append('\n')
                'f' -> out.append('\u000C')
                else -> out.append(esc)
            }
        } else {
            out.append(ch)
        }
    }
    return out.toString()
}

/**
 * Reads a properties file by the given [Path].
 */
fun Path.readProperties(): Properties = createPropertiesFromBytes(Files.readAllBytes(this))

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
