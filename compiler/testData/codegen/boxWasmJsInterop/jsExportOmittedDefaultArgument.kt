// TARGET_BACKEND: WASM
// ^^ KT-83093
// ISSUE: KT-89484
// MODULE: main
// FILE: main.kt

@JsExport
fun appendWithDefault(value: String = "K"): String = "O" + value

@JsExport
fun nullableStringWithDefault(value: String? = "default"): String = value ?: "null"

@JsExport
fun intWithDefault(x: Int = 42): Int = x

@JsExport
fun byteWithDefault(x: Byte = 7): Byte = x

@JsExport
fun charWithDefault(x: Char = 'K'): Char = x

@JsExport
fun booleanWithDefault(x: Boolean = true): Boolean = x

@JsExport
fun longWithDefault(x: Long = 5_000_000_000L): Long = x

@JsExport
fun doubleWithDefault(x: Double = 1.5): Double = x

@JsExport
fun uintWithDefault(x: UInt = 4_000_000_000u): UInt = x

@JsExport
fun nullableIntWithDefault(a: Int, b: Int? = null): Int = b ?: a

@JsExport
fun dependentDefaults(a: Int, b: Int = a + 1, c: String = "$a:$b"): String = "$a,$b,$c"

@JsExport
fun jsAnyWithDefault(x: JsAny? = "default".toJsString()): String = x?.toString() ?: "null"

@JsExport
fun lambdaWithDefault(x: Int, f: (Int) -> Int = { it + 1 }): Int = f(x)

fun box(): String {
    if (appendWithDefault() != "OK") return "Fail: appendWithDefault from Kotlin"
    if (intWithDefault() != 42) return "Fail: intWithDefault from Kotlin"
    if (dependentDefaults(1) != "1,2,1:2") return "Fail: dependentDefaults from Kotlin"
    if (dependentDefaults(1, c = "c") != "1,2,c") return "Fail: dependentDefaults with named argument from Kotlin"
    if (lambdaWithDefault(1) != 2) return "Fail: lambdaWithDefault from Kotlin"
    return "OK"
}

// FILE: entry.mjs

import {
    appendWithDefault,
    nullableStringWithDefault,
    intWithDefault,
    byteWithDefault,
    charWithDefault,
    booleanWithDefault,
    longWithDefault,
    doubleWithDefault,
    uintWithDefault,
    nullableIntWithDefault,
    dependentDefaults,
    jsAnyWithDefault,
    lambdaWithDefault,
} from "./index.mjs"

function check(actual, expected, message) {
    if (actual !== expected) {
        throw `Fail: ${message}: expected ${expected}, got ${actual}`;
    }
}

check(appendWithDefault("L"), "OL", "appendWithDefault explicit");
check(appendWithDefault(), "OK", "appendWithDefault omitted");
check(appendWithDefault(undefined), "OK", "appendWithDefault undefined");

check(nullableStringWithDefault("x"), "x", "nullableStringWithDefault explicit");
check(nullableStringWithDefault(null), "null", "nullableStringWithDefault null");
check(nullableStringWithDefault(), "default", "nullableStringWithDefault omitted");

check(intWithDefault(1), 1, "intWithDefault explicit");
check(intWithDefault(0), 0, "intWithDefault zero");
check(intWithDefault(), 42, "intWithDefault omitted");
check(intWithDefault(undefined), 42, "intWithDefault undefined");

check(byteWithDefault(1), 1, "byteWithDefault explicit");
check(byteWithDefault(), 7, "byteWithDefault omitted");

check(charWithDefault(76), 76, "charWithDefault explicit");
check(charWithDefault(), 75, "charWithDefault omitted");

check(booleanWithDefault(false), false, "booleanWithDefault explicit");
check(booleanWithDefault(), true, "booleanWithDefault omitted");

check(longWithDefault(1n), 1n, "longWithDefault explicit");
check(longWithDefault(), 5000000000n, "longWithDefault omitted");

check(doubleWithDefault(2.5), 2.5, "doubleWithDefault explicit");
check(doubleWithDefault(), 1.5, "doubleWithDefault omitted");

check(uintWithDefault(1), 1, "uintWithDefault explicit");
check(uintWithDefault(), 4000000000, "uintWithDefault omitted");

check(nullableIntWithDefault(42, 1), 1, "nullableIntWithDefault explicit");
check(nullableIntWithDefault(42, null), 42, "nullableIntWithDefault null");
check(nullableIntWithDefault(42), 42, "nullableIntWithDefault omitted");

check(dependentDefaults(1, 5, "c"), "1,5,c", "dependentDefaults explicit");
check(dependentDefaults(1, 5), "1,5,1:5", "dependentDefaults last omitted");
check(dependentDefaults(1), "1,2,1:2", "dependentDefaults two omitted");
check(dependentDefaults(1, undefined, "c"), "1,2,c", "dependentDefaults middle undefined");

check(jsAnyWithDefault("x"), "x", "jsAnyWithDefault explicit");
check(jsAnyWithDefault(null), "null", "jsAnyWithDefault null");
check(jsAnyWithDefault(), "default", "jsAnyWithDefault omitted");

check(lambdaWithDefault(1, (x) => x * 10), 10, "lambdaWithDefault explicit");
check(lambdaWithDefault(1), 2, "lambdaWithDefault omitted");
