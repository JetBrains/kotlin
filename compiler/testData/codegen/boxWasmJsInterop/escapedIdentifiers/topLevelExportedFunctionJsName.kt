// TARGET_BACKEND: WASM
// ISSUE: KT-64144

// MODULE: main
// FILE: lib.kt

@JsExport
@JsName("test:records/test#swap-tuple")
fun swapTuple(a: Int, b: Int): Int = b * 10 + a

@JsExport
@JsName("kebab-case-name")
fun kebab(): Int = 1

@JsExport
@JsName("with space")
fun withSpace(): Int = 2

@JsExport
@JsName("quote'inside")
fun singleQuote(): Int = 3

@JsExport
@JsName("1startsWithDigit")
fun startsWithDigit(): Int = 7

@JsExport
@JsName("dot.inside")
fun dotInside(): Int = 8

@JsExport
@JsName("\$valid_identifier")
fun validIdentifier(): Int = 9

@JsExport
@JsName("café")
fun unicodeIdentifier(): Int = 10

@JsExport
fun `backticked name-with@chars`(): Int = 11

fun box() = "OK"

// FILE: entry.mjs
import * as m from "./index.mjs"
import { 'test:records/test#swap-tuple' as swapTuple, 'kebab-case-name' as kebab } from "./index.mjs"

function check(name, expected, ...args) {
    const f = m[name];
    if (typeof f !== "function")
        throw "Fail: export '" + name + "' is not a function, but " + typeof f;
    const actual = f(...args);
    if (actual !== expected)
        throw "Fail: export '" + name + "' returned " + actual + " instead of " + expected;
}

if (swapTuple(1, 2) !== 21) throw "Fail: named import of 'test:records/test#swap-tuple'";
if (kebab() !== 1) throw "Fail: named import of 'kebab-case-name'";

check("test:records/test#swap-tuple", 21, 1, 2);
check("kebab-case-name", 1);
check("with space", 2);
check("quote'inside", 3);
check("1startsWithDigit", 7);
check("dot.inside", 8);
check("$valid_identifier", 9);
check("café", 10);
check("backticked name-with@chars", 11);
