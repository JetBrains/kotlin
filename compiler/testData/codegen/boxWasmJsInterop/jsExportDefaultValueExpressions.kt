// TARGET_BACKEND: WASM
// ISSUE: KT-89484
// MODULE: main
// FILE: main.kt

@JsExport
fun throwingDefault(x: Int = throw IllegalStateException("default value")): Int = x

@JsExport
fun throwingNullableDefault(x: String? = error("default value")): String = x ?: "null"

@JsExport
fun tryCatchDefault(x: Int = try { error("fail") } catch (e: IllegalStateException) { 7 }): Int = x

@JsExport
fun inlineLambdaDefault(a: Int, b: Int = listOf(1, 2, 3).first { it > a }): Int = b

@JsExport
fun anonymousObjectDefault(a: Int, b: Int = object { fun twice() = a * 2 }.twice()): Int = b

@JsExport
fun capturingLambdaDefault(a: Int, f: () -> Int = { a * 3 }): Int = f()

@JsExport
fun capturingDefaultedParameter(a: Int = 2, f: () -> Int = { a * 5 }): Int = f()

@JsExport
fun localFunctionDefault(
    a: Int,
    b: Int = run {
        fun plusOne() = a + 1
        plusOne()
    },
): Int = b

private fun secret() = 41

@JsExport
fun privateCallDefault(x: Int = secret() + 1): Int = x

@JsExport
fun recursiveDefault(n: Int, acc: Int = if (n == 0) 0 else recursiveDefault(n - 1) + n): Int = acc

@JsExport
fun whenDefault(a: Int, b: String = when (a) { 0 -> "zero"; else -> "other" }): String = b

object Holder { val v = 9 }

enum class Color { RED, GREEN }

@JsExport
fun objectDefault(x: Int = Holder.v): Int = x

@JsExport
fun enumDefault(x: String = Color.GREEN.name): String = x

var log = ""

fun record(s: String): String {
    log += s
    return s
}

@JsExport
fun orderDefault(a: String = record("a"), b: String = record("b"), c: String = record("c")): String = a + b + c

@JsExport
fun getLog(): String = log

@JsExport
fun resetLog() {
    log = ""
}

fun box(): String {
    try {
        throwingDefault()
        return "Fail: throwingDefault from Kotlin"
    } catch (e: IllegalStateException) {
        if (e.message != "default value") return "Fail: throwingDefault message from Kotlin"
    }
    if (throwingDefault(5) != 5) return "Fail: throwingDefault explicit from Kotlin"
    if (tryCatchDefault() != 7) return "Fail: tryCatchDefault from Kotlin"
    if (inlineLambdaDefault(1) != 2) return "Fail: inlineLambdaDefault from Kotlin"
    if (anonymousObjectDefault(4) != 8) return "Fail: anonymousObjectDefault from Kotlin"
    if (capturingLambdaDefault(3) != 9) return "Fail: capturingLambdaDefault from Kotlin"
    if (capturingDefaultedParameter() != 10) return "Fail: capturingDefaultedParameter from Kotlin"
    if (localFunctionDefault(4) != 5) return "Fail: localFunctionDefault from Kotlin"
    if (privateCallDefault() != 42) return "Fail: privateCallDefault from Kotlin"
    if (recursiveDefault(3) != 6) return "Fail: recursiveDefault from Kotlin"
    if (whenDefault(0) != "zero") return "Fail: whenDefault from Kotlin"
    if (objectDefault() != 9) return "Fail: objectDefault from Kotlin"
    if (enumDefault() != "GREEN") return "Fail: enumDefault from Kotlin"
    resetLog()
    if (orderDefault(b = "B") != "aBc" || log != "ac") return "Fail: orderDefault from Kotlin"
    resetLog()
    return "OK"
}

// FILE: entry.mjs

import {
    throwingDefault,
    throwingNullableDefault,
    tryCatchDefault,
    inlineLambdaDefault,
    anonymousObjectDefault,
    capturingLambdaDefault,
    capturingDefaultedParameter,
    localFunctionDefault,
    privateCallDefault,
    recursiveDefault,
    whenDefault,
    objectDefault,
    enumDefault,
    orderDefault,
    getLog,
    resetLog,
} from "./index.mjs"

function check(actual, expected, message) {
    if (actual !== expected) {
        throw `Fail: ${message}: expected ${expected}, got ${actual}`;
    }
}

function throws(f) {
    try {
        f();
        return false;
    } catch (e) {
        return true;
    }
}

check(throwingDefault(5), 5, "throwingDefault explicit");
check(throws(() => throwingDefault()), true, "throwingDefault omitted throws");
check(throws(() => throwingDefault(undefined)), true, "throwingDefault undefined throws");

check(throwingNullableDefault("x"), "x", "throwingNullableDefault explicit");
check(throwingNullableDefault(null), "null", "throwingNullableDefault null");
check(throws(() => throwingNullableDefault()), true, "throwingNullableDefault omitted throws");

check(tryCatchDefault(), 7, "tryCatchDefault omitted");
check(inlineLambdaDefault(1), 2, "inlineLambdaDefault omitted");
check(anonymousObjectDefault(4), 8, "anonymousObjectDefault omitted");
check(capturingLambdaDefault(3), 9, "capturingLambdaDefault omitted");
check(capturingDefaultedParameter(), 10, "capturingDefaultedParameter omitted");
check(capturingDefaultedParameter(3), 15, "capturingDefaultedParameter first explicit");
check(localFunctionDefault(4), 5, "localFunctionDefault omitted");
check(privateCallDefault(), 42, "privateCallDefault omitted");
check(recursiveDefault(3), 6, "recursiveDefault omitted");
check(recursiveDefault(3, 100), 100, "recursiveDefault explicit");
check(whenDefault(0), "zero", "whenDefault omitted");
check(whenDefault(1), "other", "whenDefault omitted other");
check(objectDefault(), 9, "objectDefault omitted");
check(enumDefault(), "GREEN", "enumDefault omitted");

check(throws(() => privateCallDefault(null)), true, "privateCallDefault null for non-nullable Int throws");
check(throws(() => enumDefault(null)), true, "enumDefault null for non-nullable String throws");

resetLog();
check(orderDefault(), "abc", "orderDefault omitted");
check(getLog(), "abc", "orderDefault evaluation order");
resetLog();
check(orderDefault(undefined, "B"), "aBc", "orderDefault middle explicit");
check(getLog(), "ac", "orderDefault evaluates only omitted defaults");
