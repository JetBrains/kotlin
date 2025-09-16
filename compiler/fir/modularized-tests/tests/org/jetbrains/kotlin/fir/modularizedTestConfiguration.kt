/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * Factory functions for ModularizedTestConfig moved to runners package.
 */
package org.jetbrains.kotlin.fir

import java.io.File

data class ModularizedTestConfig(
    val rootPathPrefix: String = "/",
    val outputDirRegexFilter: String = ".*",
    val moduleNameFilter: String? = null,
    val moduleNameRegexOut: String? = null,
    val containsSourcesRegexFilter: String? = null,
    val enableSlowAssertions: Boolean = false,
    val reportTimestamp: Long? = null,
    val jpsDir: String? = null,
    val jvmTarget: String = "1.8",
    val kotlinHome: String? = null,
    val composePluginClasspath: String? = null,
)

fun modularizedTestConfigFromSingleModelFile(modelFile: File): ModularizedTestConfig = ModularizedTestConfig(
    rootPathPrefix = modelFile.absoluteFile.parentFile.parentFile.path + "/",
)

fun modularizedTestConfigFromSystemProperties(): ModularizedTestConfig = ModularizedTestConfig(
    rootPathPrefix = System.getProperty("fir.bench.prefix", "/"),
    outputDirRegexFilter = System.getProperty("fir.bench.filter", ".*"),
    moduleNameFilter = System.getProperty("fir.bench.filter.name"),
    moduleNameRegexOut = System.getProperty("fir.bench.filter.out.name"),
    containsSourcesRegexFilter = System.getProperty("fir.bench.filter.contains.sources"),
    enableSlowAssertions = System.getProperty("fir.bench.enable.slow.assertions") == "true",
    reportTimestamp = System.getProperty("fir.bench.report.timestamp")?.toLongOrNull(),
    jpsDir = System.getProperty("fir.bench.jps.dir"),
    jvmTarget = System.getProperty("fir.bench.jvm.target", "1.8"),
    kotlinHome = null,
    composePluginClasspath = System.getProperty("fir.bench.compose.plugin.classpath"),
)

/**
 * Create [ModularizedTestConfig] from CLI arguments. Arguments may be provided using any of:
 *  - --key=value
 *  - -Dkey=value
 *  - key=value
 *  - --key or -Dkey (boolean flags will be treated as true)
 * The keys must match the system property names listed in [modularizedTestConfigFromSystemProperties].
 */
fun modularizedTestConfigFromArgs(args: Array<String>): ModularizedTestConfig {
    val map = mutableMapOf<String, String?>()

    fun putRaw(argBody: String) {
        val idx = argBody.indexOf('=')
        if (idx >= 0) {
            val key = argBody.substring(0, idx)
            val value = argBody.substring(idx + 1)
            map[key] = value
        } else {
            // No value provided: treat as boolean flag set to true
            map[argBody] = "true"
        }
    }

    for (arg in args) {
        when {
            arg.startsWith("-D") && arg.length > 2 -> putRaw(arg.substring(2))
            arg.startsWith("--") && arg.length > 2 -> putRaw(arg.substring(2))
            '=' in arg -> putRaw(arg)
            else -> {
                // Unsupported form, ignore silently to keep compatibility
            }
        }
    }

    fun str(name: String, default: String?): String? = map[name] ?: default
    fun strNonNull(name: String, default: String): String = map[name] ?: default
    fun long(name: String): Long? = map[name]?.toLongOrNull()
    fun bool(name: String, default: Boolean): Boolean = when (val v = map[name]) {
        null -> default
        "" -> default
        else -> v.equals("true", ignoreCase = true) || v == "1" || v.equals("yes", ignoreCase = true)
    }

    return ModularizedTestConfig(
        rootPathPrefix = strNonNull("fir.bench.prefix", "/"),
        outputDirRegexFilter = strNonNull("fir.bench.filter", ".*"),
        moduleNameFilter = str("fir.bench.filter.name", null),
        moduleNameRegexOut = str("fir.bench.filter.out.name", null),
        containsSourcesRegexFilter = str("fir.bench.filter.contains.sources", null),
        enableSlowAssertions = bool("fir.bench.enable.slow.assertions", false),
        reportTimestamp = long("fir.bench.report.timestamp"),
        jpsDir = str("fir.bench.jps.dir", null),
        jvmTarget = strNonNull("fir.bench.jvm.target", "1.8"),
        kotlinHome = str("fir.bench.kotlin.home", null),
        composePluginClasspath = str("fir.bench.compose.plugin.classpath", null),
    )
}

fun modularizedTestConfigFromArgsOrSystemProperties(args: Array<String>) =
    if (args.isEmpty()) modularizedTestConfigFromSystemProperties()
    else modularizedTestConfigFromArgs(args)