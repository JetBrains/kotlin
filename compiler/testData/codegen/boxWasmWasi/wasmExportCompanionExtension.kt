// LANGUAGE: +CompanionBlocks +CompanionExtensions
// MODULE: main
// FILE: main.kt

import kotlin.wasm.WasmExport

class Test {
    companion {
        val base = 40
    }
}

@WasmExport
companion fun Test.companionExtension(value: Int): Int = Test.base + value

fun box() = "OK"

// FILE: entry.mjs

import { companionExtension } from "./index.mjs"

if (companionExtension(2) !== 42) throw "FAIL: companion extension"
