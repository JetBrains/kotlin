/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.config.*
import java.lang.reflect.Field
import java.util.regex.Matcher
import java.util.regex.Pattern

private val BOOLEAN_FLAG_PATTERN = Pattern.compile("([+-])(([a-zA-Z_0-9]*)\\.)?([a-zA-Z_0-9]*)")

@OptIn(ExperimentalStdlibApi::class)
private val patterns = buildList {
    createPattern(
        "ASSERTIONS_MODE",
        JVMConfigurationKeys.ASSERTIONS_MODE,
        JVMAssertionsMode.Companion::fromString
    )
    createPattern(
        "STRING_CONCAT",
        JVMConfigurationKeys.STRING_CONCAT,
        JvmStringConcat.Companion::fromString
    )
    createPattern(
        "SAM_CONVERSIONS",
        JVMConfigurationKeys.SAM_CONVERSIONS,
        JvmClosureGenerationScheme.Companion::fromString
    )
    createPattern(
        "LAMBDAS",
        JVMConfigurationKeys.LAMBDAS,
        JvmClosureGenerationScheme.Companion::fromString
    )
    createPattern(
        "USE_OLD_INLINE_CLASSES_MANGLING_SCHEME",
        JVMConfigurationKeys.USE_OLD_INLINE_CLASSES_MANGLING_SCHEME,
    )
    createPattern(
        "SERIALIZE_IR",
        JVMConfigurationKeys.SERIALIZE_IR,
        JvmSerializeIrMode.Companion::fromString,
    )
}

private sealed class PatternWithExtractor<E : Any> {
    abstract val configurationKey: CompilerConfigurationKey<E>
    abstract val pattern: Pattern

    abstract fun extract(matcher: Matcher): E
}

private class ValuePatternWithExtractor<E : Any>(
    val directive: String,
    override val configurationKey: CompilerConfigurationKey<E>,
    val extractor: (String) -> E?
) : PatternWithExtractor<E>() {
    override val pattern: Pattern = Pattern.compile("$directive=([a-zA-Z_0-9-]*)")

    override fun extract(matcher: Matcher): E {
        val stringValue = matcher.group(1)
        return extractor(stringValue) ?: error("Wrong $directive value: $stringValue")
    }
}

private class BooleanPatternWithExtractor(
    val directive: String,
    override val configurationKey: CompilerConfigurationKey<Boolean>
) : PatternWithExtractor<Boolean>() {
    override val pattern: Pattern = Pattern.compile(directive)

    override fun extract(matcher: Matcher): Boolean {
        return true
    }
}

private fun <E : Any> MutableList<PatternWithExtractor<*>>.createPattern(
    directive: String,
    configurationKey: CompilerConfigurationKey<E>,
    extractor: (String) -> E?,
): PatternWithExtractor<E> {
    return ValuePatternWithExtractor(directive, configurationKey, extractor).also { this += it }
}

private fun MutableList<PatternWithExtractor<*>>.createPattern(
    directive: String,
    configurationKey: CompilerConfigurationKey<Boolean>
): PatternWithExtractor<Boolean> {
    return BooleanPatternWithExtractor(directive, configurationKey).also { this += it }
}

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

    @Suppress("unused")
    for (flag in rawFlags) {
        var m = BOOLEAN_FLAG_PATTERN.matcher(flag)
        if (m.matches()) {
            val flagEnabled = "-" != m.group(1)
            val flagNamespace = m.group(3)
            val flagName = m.group(4)
            tryApplyBooleanFlag(result, flag, flagEnabled, flagNamespace, flagName)
            continue
        }
        for (pattern in patterns) {
            m = pattern.pattern.matcher(flag)
            if (m.matches()) {
                result[pattern.configurationKey] = pattern.extract(m)
                continue
            }
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
        destination[configurationKey] = flagEnabled
    } catch (e: java.lang.Exception) {
        assert(false) { "Expected [+|-][namespace.]configurationKey, got: $flag" }
    }
}
