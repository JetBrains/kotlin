/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.utils

import org.jetbrains.kotlin.cli.pipeline.web.wasm.WasmCompilationMode

/**
 * For each of these, *null means all*, e.g., if `os` is null, the config expects the test to fail regardless of the OS
 */
data class WasmIgnoreForConfig(
    val mode: WasmCompilationMode? = null,
    val os: String? = null,
    val vmName: String? = null,
) {
    override fun toString(): String {
        val props = listOfNotNull(
            mode?.let { "mode=$it" },
            os?.let { "os=$it" },
            vmName?.let { "vm=$it" }).joinToString(" ")
        return "WASM_IGNORE_FOR: $props"
    }
}

fun wasmIgnoreForParser(raw: String): WasmIgnoreForConfig? {
    // sanity check: no duplicates, neither in keys (no duplicate 'vm='), nor values (luckily, os, mode, and vm don't overlap in their sets of valid values)
    val individualParts = raw.split(' ').flatMap { it.split('=', limit = 2) }
    if (individualParts.distinct().size != individualParts.size) {
        System.err.println("`WASM_IGNORE_FOR` directive arguments '$raw' contain duplicate property assignments, which is not allowed.\nTo ignore based on a logical OR condition, use two separate `WASM_IGNORE_FOR` directives.")
        return null
    }

    val parts = raw.split(' ').associate {
        val splitList = it.split("=", limit = 2)
        // invalid syntax
        if (splitList.size != 2) return null

        val (k, v) = splitList
        k to v
    }

    // sanitizing
    if (parts.isEmpty()) {
        System.err.println("Directive $raw does not specify any properties to base the suppressor on.\nIf this is an intentional catch-all suppression, use IGNORE_BACKEND")
        return null
    }
    if (parts.keys.any { it !in listOf("mode", "os", "vm") }) {
        System.err.println("Invalid key specified in directive $raw, only know keys 'mode', 'os', 'vm'")
        return null
    }
    if (parts["os"]?.lowercase() !in listOf(null, "linux", "windows", "mac")) {
        System.err.println("Invalid OS specified in WASM_IGNORE_FOR directive: os=${parts["os"]}. Must be linux, windows, or mac (case insensitive)")
        return null
    }
    // NOTE: mode mismatch will be caught by WasmCompilationMode.valueOf
    // NOTE: vm mismatches will be caught by the test itself, i.e. it will fail, or warn that it should be unmuted,
    //       if the config is wrong.
    //       There's unfortunately no non-hardcoded way to check all WasmVMs, without kotlin-reflections,
    //       and adding a module dependency on the testFixtures module.

    return WasmIgnoreForConfig(
        mode = parts["mode"]?.let { WasmCompilationMode.valueOf(it.uppercase().replace('-', '_')) },
        os = parts["os"]?.lowercase(),
        vmName = parts["vm"],
    )
}

