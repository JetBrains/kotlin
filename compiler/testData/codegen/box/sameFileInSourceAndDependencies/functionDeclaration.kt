// NATIVE error: static cache is broken: ld.gold invocation reported errors. Please try to disable compiler caches and rerun the build.
// DONT_TARGET_EXACT_BACKEND: NATIVE
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
