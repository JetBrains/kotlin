// TARGET_BACKEND: JS
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: JS_IR_ES6
// TARGET_BACKEND: WASM

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
    return if (l != "Foo;Foo.CO;") "FAIL: ${l}" else "OK"
}
