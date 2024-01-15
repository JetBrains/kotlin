// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_ASSERT
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// IGNORE_BACKEND: JS
// See related issue: https://youtrack.jetbrains.com/issue/KT-59059/Native-Assert-does-not-evaluate-argument-value-when-disabled
// ASSERTIONS_MODE: always-disable
// WITH_STDLIB

@file:Suppress("OPT_IN_USAGE_ERROR") // ExperimentalNativeApi is defined only in Native

fun checkTrue(): Boolean {
    var hit = false
    val l = { hit = true; true }
    assert(l())
    return hit
}

fun checkTrueWithMessage(): Boolean {
    var hit = false
    val l = { hit = true; true }
    assert(l()) { "BOOYA!" }
    return hit
}

fun checkFalse(): Boolean {
    var hit = false
    val l = { hit = true; false }
    assert(l())
    return hit
}

fun checkFalseWithMessage(): Boolean {
    var hit = false
    val l = { hit = true; false }
    assert(l()) { "BOOYA!" }
    return hit
}

fun box(): String {
    if (checkTrue()) return "FAIL 0"
    if (checkTrueWithMessage()) return "FAIL 1"
    if (checkFalse()) return "FAIL 2"
    if (checkFalseWithMessage()) return "FAIL 3"

    return "OK"
}
