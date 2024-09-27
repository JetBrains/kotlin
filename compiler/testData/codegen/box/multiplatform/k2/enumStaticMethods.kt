// IGNORE_BACKEND: WASM, JS, NATIVE, JS_IR, JS_IR_ES6
// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

expect enum class E {
    O, K;

    fun values(): Int
}

fun common(): String {
    return if (E.O.values() == 42) E.valueOf("O").name + E.valueOf("K").name else "NOT OK"
}

// MODULE: platform()()(common)
// FILE: platform.kt

actual enum class E {
    O, K;

    // Not to be confused with `static fun values`
    actual fun values(): Int = 42
}

fun box(): String = common()
