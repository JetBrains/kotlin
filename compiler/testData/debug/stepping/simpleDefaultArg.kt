
// FILE: test.kt

fun ifoo(ok: String = "OK"): String {
    return ok
}

fun box(): String {
    return ifoo()
}

// FORCE_STEP_INTO
// EXPECTATIONS JVM_IR
// test.kt:9 box
// test.kt:4 ifoo$default (synthetic)
// test.kt:5 ifoo
// test.kt:4 ifoo$default (synthetic)
// test.kt:9 box

// EXPECTATIONS JS_IR
// test.kt:9 box

// EXPECTATIONS WASM
// test.kt:9 $box (11, 11, 11, 11, 4)
// test.kt:4 $ifoo$default (0, 0, 0, 22, 0)
// test.kt:5 $ifoo (11, 4)
