// DONT_TARGET_EXACT_BACKEND: JS
// DONT_TARGET_EXACT_BACKEND: JS_IR
// DONT_TARGET_EXACT_BACKEND: JS_IR_ES6
// DONT_TARGET_EXACT_BACKEND: WASM

var l = ""
enum class Foo {
    F;
    init {
        l += "Foo;"
    }
    object L {
        init {
            l += "Foo.CO;"
        }
    }
}

fun box(): String {
    Foo.L
    return if (l != "Foo.CO;") "FAIL: ${l}" else "OK"
}
