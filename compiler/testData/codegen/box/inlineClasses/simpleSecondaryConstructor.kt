// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS, JS_IR, NATIVE, JVM
// IGNORE_BACKEND: JS_IR_ES6
// !LANGUAGE: +InlineClasses

@file:Suppress("SECONDARY_CONSTRUCTOR_WITH_BODY_INSIDE_INLINE_CLASS")

inline class Foo(val x: String) {
    constructor(y: Int) : this("OK") {
        if (y == 0) return throw java.lang.IllegalArgumentException()
        if (y == 1) return
        return Unit
    }

    constructor(z: Double) : this(z.toInt())
}

fun box(): String {
    return Foo(42.0).x
}
