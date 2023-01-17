// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// MODULE: lib
// FILE: 2.kt
fun a() = "OK"
fun b() = a()

// FILE: 3.kt
fun c() = b()

// MODULE: main(lib)
// FILE: 1.kt
fun d() = c()

fun box(): String = d()

// FILE: 2.kt
fun a() = "OK"
fun b() = a()
