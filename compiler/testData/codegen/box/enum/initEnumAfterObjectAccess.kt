// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: Wasm-js:2.3,2.4
// ^KT-83337 Difference in behavior on nested class initialization

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
    val expected = when (BACKEND_UNDER_TEST) {
        // KT-83337 Difference in behavior on nested class initialization
        "JS_IR", "JS_IR_ES6" -> "Foo;Foo.CO;"
        else                 -> "Foo.CO;"
    }
    return if (l != expected) "FAIL: ${l}" else "OK"
}
