// FILE: test.kt

fun ifoo(ok: String = "OK"): String {
    return ok
}

fun box(): String {
    return ifoo()
}

// FORCE_STEP_INTO
// EXPECTATIONS JVM JVM_IR
// test.kt:8 box
// test.kt:3 ifoo$default (synthetic)
// test.kt:4 ifoo
// test.kt:3 ifoo$default (synthetic)
// test.kt:8 box

// EXPECTATIONS JS_IR
// test.kt:8 box