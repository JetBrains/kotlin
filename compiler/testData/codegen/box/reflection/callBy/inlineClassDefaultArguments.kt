// IGNORE_BACKEND: JS_IR, JS, NATIVE, WASM
// IGNORE_BACKEND: JS_IR_ES6
// WITH_REFLECT

inline class A(val x: Int)

fun test(x: A = A(0)) = "OK"

fun box(): String {
    return (::test).callBy(mapOf())
}
