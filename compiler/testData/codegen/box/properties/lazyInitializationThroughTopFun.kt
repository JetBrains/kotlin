// TARGET_BACKEND: JS_IR
// IGNORE_BACKEND: WASM
// PROPERTY_LAZY_INITIALIZATION

// FILE: A.kt
var result: String? = null

val a = "a".let {
    result = "OK"
    it + "a"
}

fun foo() =
    2 + 2

// FILE: main.kt
fun box(): String {
    val foo = foo()
    return result!!
}