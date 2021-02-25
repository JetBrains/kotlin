/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm


import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledModuleFragment
import org.jetbrains.kotlin.backend.wasm.ir2wasm.generateStringLiteralsSupport

class WasmCompilerResult(val wat: String, val js: String, val wasm: ByteArray)

fun WasmCompiledModuleFragment.generateJs(): String {
    val runtime = """
    const runtime = {
        String_getChar(str, index) {
            return str.charCodeAt(index);
        },

        String_compareTo(str1, str2) {
            if (str1 > str2) return 1;
            if (str1 < str2) return -1;
            return 0;
        },

        String_equals(str, other) {
            return str === other;
        },

        String_subsequence(str, startIndex, endIndex) {
            return str.substring(startIndex, endIndex);
        },

        String_getLiteral(index) {
            return runtime.stringLiterals[index];
        },

        coerceToString(value) {
            return String(value);
        },

        Char_toString(char) {
            return String.fromCharCode(char)
        },

        identity(x) {
            return x;
        },

        println(value) {
            console.log(">>>  " + value)
        }
    };
    """.trimIndent()

    val jsCode =
        "\nconst js_code = {${jsFuns.joinToString(",\n") { "\"" + it.importName + "\" : " + it.jsCode }}};"

    return runtime + generateStringLiteralsSupport(stringLiterals) + jsCode
}
