// TARGET_BACKEND: JS_IR
// DONT_TARGET_EXACT_BACKEND: WASM
// PROPERTY_LAZY_INITIALIZATION

// FILE: A.kt
val a = "a".let {
    it + "a"
}

fun foo() =
    2 + 2

// FILE: main.kt
fun box(): String {
    val foo = foo()
    return if (js("typeof a") == "string" && js("a") == "aa") "OK" else "fail"
}