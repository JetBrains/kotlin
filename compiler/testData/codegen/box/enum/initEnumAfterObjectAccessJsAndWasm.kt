// DONT_TARGET_EXACT_BACKEND: JVM
// DONT_TARGET_EXACT_BACKEND: JVM_IR
// DONT_TARGET_EXACT_BACKEND: NATIVE
// DONT_TARGET_EXACT_BACKEND: WASM_JS, WASM_WASI
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
    return if (l != "Foo;Foo.CO;") "FAIL: ${l}" else "OK"
}
