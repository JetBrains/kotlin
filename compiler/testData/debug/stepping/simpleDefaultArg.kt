// IGNORE_BACKEND: WASM
// FILE: test.kt

fun ifoo(ok: String = "OK"): String {
    return ok
}

fun box(): String {
    return ifoo()
}

// FORCE_STEP_INTO
// EXPECTATIONS JVM JVM_IR
// test.kt:9 box
// test.kt:4 ifoo$default (synthetic)
// test.kt:5 ifoo
// test.kt:4 ifoo$default (synthetic)
// test.kt:9 box

// EXPECTATIONS JS_IR
// test.kt:9 box