

// FILE: test.kt

inline fun ifoo(ok: String = "OK"): String {
    return ok
}

fun ifoo2(ok: String = "OK"): String {
    return ok
}

fun box(): String {
    ifoo()
    return ifoo2()
}

// FORCE_STEP_INTO
// EXPECTATIONS JVM_IR
// test.kt:14 box
// test.kt:5 box
// test.kt:6 box
// test.kt:15 box
// test.kt:9 ifoo2$default (synthetic)
// test.kt:10 ifoo2
// test.kt:9 ifoo2$default (synthetic)
// test.kt:15 box

// EXPECTATIONS JS_IR
// test.kt:15 box

// EXPECTATIONS WASM
// test.kt:14 $box
// test.kt:5 $box (29, 29)
// test.kt:6 $box (11, 4)
// test.kt:15 $box (11, 11, 11, 11, 4)
// test.kt:9 $ifoo2$default (0, 0, 0, 23, 0)
// test.kt:10 $ifoo2 (11, 4)
