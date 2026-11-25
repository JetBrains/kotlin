// TARGET_BACKEND: WASM
// ISSUE: KT-64144

// MODULE: main
// FILE: lib.kt

import kotlin.wasm.WasmExport

@WasmExport("wasm:records/test#swap-tuple")
fun swapTuple(a: Int, b: Int): Int = b * 10 + a

@WasmExport("wasm-kebab-case")
fun kebab(): Int = 1

@WasmExport("wasm with space")
fun withSpace(): Int = 2

@WasmExport("wasm'quote")
fun singleQuote(): Int = 3

@WasmExport("2startsWithDigit")
fun startsWithDigit(): Int = 7

@WasmExport("wasm.dot")
fun dotInside(): Int = 8

@WasmExport
fun `wasm backticked@name`(): Int = 9

fun box() = "OK"

// FILE: entry.mjs
import * as m from "./index.mjs"
import { 'wasm:records/test#swap-tuple' as swapTuple, 'wasm-kebab-case' as kebab } from "./index.mjs"

function check(name, expected, ...args) {
    const f = m[name];
    if (typeof f !== "function")
        throw "Fail: export '" + name + "' is not a function, but " + typeof f;
    const actual = f(...args);
    if (actual !== expected)
        throw "Fail: export '" + name + "' returned " + actual + " instead of " + expected;
}

if (swapTuple(1, 2) !== 21) throw "Fail: named import of 'wasm:records/test#swap-tuple'";
if (kebab() !== 1) throw "Fail: named import of 'wasm-kebab-case'";

check("wasm:records/test#swap-tuple", 21, 1, 2);
check("wasm-kebab-case", 1);
check("wasm with space", 2);
check("wasm'quote", 3);
check("2startsWithDigit", 7);
check("wasm.dot", 8);
check("wasm backticked@name", 9);
