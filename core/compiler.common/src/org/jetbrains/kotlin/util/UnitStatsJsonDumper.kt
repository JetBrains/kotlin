/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import kotlin.reflect.KProperty

object UnitStatsJsonDumper {
    fun dump(stats: UnitStats): String {
        with(stats) {
            return buildString {
                appendArrayElement(this, indent = 0, trailingComma = false) {
                    name?.let { appendKeyValue(::name, it, indent = 1) }
                    outputKind?.let { appendKeyValue(::outputKind, it, indent = 1) }
                    appendKeyValue(::timeStampMs, timeStampMs, indent = 1)

                    // Unfortunately, it's not possible to ignore the properties `platform`, `compilerType`, `hasErrors` if they have default values
                    // because Gson library (that used in build tools) deserializes them to real `null` instead of default values that causes NRE on valid code
                    appendKeyValue(::platform, platform.name, indent = 1)
                    appendKeyValue(::compilerType, compilerType.name, indent = 1)
                    appendKeyValue(::hasErrors, hasErrors, indent = 1)

                    appendKeyValue(::filesCount, filesCount, indent = 1)
                    appendKeyValue(::linesCount, linesCount, indent = 1)

                    initStats?.let { appendTime(::initStats, it, indent = 1, trailingComma = true) }
                    analysisStats?.let { appendTime(::analysisStats, it, indent = 1, trailingComma = true) }
                    translationToIrStats?.let { appendTime(::translationToIrStats, it, indent = 1, trailingComma = true) }
                    irPreLoweringStats?.let { appendTime(::irPreLoweringStats, it, indent = 1, trailingComma = true) }
                    irSerializationStats?.let { appendTime(::irSerializationStats, it, indent = 1, trailingComma = true) }
                    klibWritingStats?.let { appendTime(::klibWritingStats, it, indent = 1, trailingComma = true) }
                    irLoweringStats?.let { appendTime(::irLoweringStats, it, indent = 1, trailingComma = true) }
                    backendStats?.let { appendTime(::backendStats, it, indent = 1, trailingComma = true) }

                    dynamicStats?.let {
                        appendArray(::dynamicStats, dynamicStats, indent = 1, trailingComma = true) {
                            appendKeyValue(it::parentPhaseType, it.parentPhaseType, indent = 3)
                            appendKeyValue(it::name, it.name, indent = 3)
                            appendTime(it::time, it.time, indent = 3, trailingComma = false)
                        }
                    }

                    klibElementStats?.let {
                        appendArray(::klibElementStats, klibElementStats, indent = 1, trailingComma = true) {
                            appendKeyValue(it::path, it.path, indent = 3)
                            appendKeyValue(it::size, it.size, indent = 3, trailingComma = false)
                        }
                    }

                    findJavaClassStats?.let { appendSideStats(::findJavaClassStats, it, indent = 1, trailingComma = true) }
                    findKotlinClassStats?.let { appendSideStats(::findKotlinClassStats, it, indent = 1, trailingComma = true) }

                    appendArray(::gcStats, gcStats, indent = 1, trailingComma = true) {
                        appendKeyValue(it::kind, it.kind, indent = 3)
                        appendKeyValue(it::count, it.count, indent = 3)
                        appendKeyValue(it::millis, it.millis, indent = 3, trailingComma = false)
                    }

                    if (jitTimeMillis != null) {
                        appendKeyValue(::jitTimeMillis, jitTimeMillis, indent = 1)
                    }

                    // Ignore deprecated `extendedStats` that used in K1 only

                    if (endsWith(TRAILING_COMMA_SUFFIX)) {
                        replace(length - TRAILING_COMMA_SUFFIX.length, length, "\n")
                    }
                }
            }
        }
    }

    private const val TRAILING_COMMA_SUFFIX = ",\n"

    private fun StringBuilder.appendSideStats(key: KProperty<*>, sideStats: SideStats, indent: Int = 1, trailingComma: Boolean = true) {
        appendObject(key, sideStats, indent, trailingComma) {
            sideStats.apply {
                appendKeyValue(::count, count, indent + 1)
                appendTime(::time, time, indent + 1, trailingComma = false)
            }
        }
    }

    private fun StringBuilder.appendTime(key: KProperty<*>, time: Time, indent: Int, trailingComma: Boolean) {
        appendObject(key, time, indent, trailingComma) {
            appendKeyValue(it::nanos, it.nanos, indent + 1)
            appendKeyValue(it::userNanos, it.userNanos, indent + 1)
            appendKeyValue(it::cpuNanos, it.cpuNanos, indent + 1, trailingComma = false)
        }
    }

    private fun <T> StringBuilder.appendArray(
        key: KProperty<*>,
        array: List<T>,
        indent: Int = 1,
        trailingComma: Boolean,
        appendObjectContent: (T) -> Unit
    ) {
        appendInternal("\"${key.name}\": [", indent, trailingComma = false)
        for ((index, item) in array.withIndex()) {
            appendArrayElement(item, indent + 1, trailingComma = index < array.size - 1, appendObjectContent)
        }
        appendInternal("]", indent, trailingComma)
    }

    private fun <T> StringBuilder.appendArrayElement(value: T, indent: Int, trailingComma: Boolean, appendObjectContent: (T) -> Unit) {
        appendObject(key = null, value, indent, trailingComma, appendObjectContent)
    }

    private fun <T> StringBuilder.appendObject(
        key: KProperty<*>?,
        value: T,
        indent: Int,
        trailingComma: Boolean,
        appendObjectContent: (T) -> Unit
    ) {
        if (key != null)
            appendInternal("\"${key.name}\": {", indent, trailingComma = false)
        else
            appendInternal("{", indent, trailingComma = false)
        appendObjectContent(value)
        appendInternal("}", indent, trailingComma)
    }

    private fun StringBuilder.appendKeyValue(key: KProperty<*>, value: Any, indent: Int = 1, trailingComma: Boolean = true) {
        val valueInfix = if (value is String) "\"$value\"" else value.toString()
        appendInternal("\"${key.name}\": $valueInfix", indent, trailingComma)
    }

    private fun StringBuilder.appendInternal(s: String, indent: Int, trailingComma: Boolean) {
        repeat(indent) { append("  ") }
        append(s)
        if (trailingComma) {
            append(',')
        }
        append('\n')
    }
}