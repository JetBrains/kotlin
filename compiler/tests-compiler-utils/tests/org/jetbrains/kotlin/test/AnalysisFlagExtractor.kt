/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.config.*
import java.lang.reflect.Field
import java.util.regex.Pattern

private val BOOLEAN_FLAG_PATTERN = Pattern.compile("([+-])(([a-zA-Z_0-9]*)\\.)?([a-zA-Z_0-9]*)")
private val CONSTRUCTOR_CALL_NORMALIZATION_MODE_FLAG_PATTERN = Pattern.compile(
    "CONSTRUCTOR_CALL_NORMALIZATION_MODE=([a-zA-Z_\\-0-9]*)"
)
private val ASSERTIONS_MODE_FLAG_PATTERN = Pattern.compile("ASSERTIONS_MODE=([a-zA-Z_0-9-]*)")
private val STRING_CONCAT = Pattern.compile("STRING_CONCAT=([a-zA-Z_0-9-]*)")


private val FLAG_CLASSES: List<Class<*>> = listOf(
    CLIConfigurationKeys::class.java,
    JVMConfigurationKeys::class.java
)

private val FLAG_NAMESPACE_TO_CLASS: Map<String, Class<*>> = mapOf(
    "CLI" to CLIConfigurationKeys::class.java,
    "JVM" to JVMConfigurationKeys::class.java
)

fun parseAnalysisFlags(rawFlags: List<String>): Map<CompilerConfigurationKey<*>, Any> {
    val result = mutableMapOf<CompilerConfigurationKey<*>, Any>()

    for (flag in rawFlags) {
        var m = BOOLEAN_FLAG_PATTERN.matcher(flag)
        if (m.matches()) {
            val flagEnabled = "-" != m.group(1)
            val flagNamespace = m.group(3)
            val flagName = m.group(4)
            tryApplyBooleanFlag(result, flag, flagEnabled, flagNamespace, flagName)
            continue
        }
        m = CONSTRUCTOR_CALL_NORMALIZATION_MODE_FLAG_PATTERN.matcher(flag)
        if (m.matches()) {
            val flagValueString = m.group(1)
            val mode = JVMConstructorCallNormalizationMode.fromStringOrNull(flagValueString)
                ?: error("Wrong CONSTRUCTOR_CALL_NORMALIZATION_MODE value: $flagValueString")
            result[JVMConfigurationKeys.CONSTRUCTOR_CALL_NORMALIZATION_MODE] = mode
        }
        m = ASSERTIONS_MODE_FLAG_PATTERN.matcher(flag)
        if (m.matches()) {
            val flagValueString = m.group(1)
            val mode = JVMAssertionsMode.fromStringOrNull(flagValueString)
                ?: error("Wrong ASSERTIONS_MODE value: $flagValueString")
            result[JVMConfigurationKeys.ASSERTIONS_MODE] = mode
        }

        m = STRING_CONCAT.matcher(flag)
        if (m.matches()) {
            val flagValueString = m.group(1)
            val mode = JvmStringConcat.fromString(flagValueString)
                ?: error("Wrong STRING_CONCAT value: $flagValueString")
            result[JVMConfigurationKeys.STRING_CONCAT] = mode
        }
    }

    return result
}

private fun tryApplyBooleanFlag(
    destination: MutableMap<CompilerConfigurationKey<*>, Any>,
    flag: String,
    flagEnabled: Boolean,
    flagNamespace: String?,
    flagName: String
) {
    val configurationKeysClass: Class<*>?
    var configurationKeyField: Field? = null
    if (flagNamespace == null) {
        for (flagClass in FLAG_CLASSES) {
            try {
                configurationKeyField = flagClass.getField(flagName)
                break
            } catch (ignored: java.lang.Exception) {
            }
        }
    } else {
        configurationKeysClass = FLAG_NAMESPACE_TO_CLASS[flagNamespace]
        assert(configurationKeysClass != null) { "Expected [+|-][namespace.]configurationKey, got: $flag" }
        configurationKeyField = try {
            configurationKeysClass!!.getField(flagName)
        } catch (e: java.lang.Exception) {
            null
        }
    }
    assert(configurationKeyField != null) { "Expected [+|-][namespace.]configurationKey, got: $flag" }
    try {
        @Suppress("UNCHECKED_CAST")
        val configurationKey = configurationKeyField!![null] as CompilerConfigurationKey<Boolean>
        destination.put(configurationKey, flagEnabled)
    } catch (e: java.lang.Exception) {
        assert(false) { "Expected [+|-][namespace.]configurationKey, got: $flag" }
    }
}


