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
// LINENUMBERS
// test.kt:12 box
// test.kt:3 box
// test.kt:4 box
// test.kt:13 box
// test.kt:7 ifoo2$default (synthetic)
// test.kt:8 ifoo2
// test.kt:7 ifoo2$default (synthetic)
// test.kt:13 box