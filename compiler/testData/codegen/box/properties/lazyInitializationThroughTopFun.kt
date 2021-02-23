// TARGET_BACKEND: JS_IR
// DONT_TARGET_EXACT_BACKEND: WASM
// PROPERTY_LAZY_INITIALIZATION


var result: String? = null

// FILE: A.kt
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