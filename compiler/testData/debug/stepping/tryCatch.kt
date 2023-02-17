// IGNORE_BACKEND_K2_LIGHT_TREE: JVM_IR
//   Reason: KT-56755
// FILE: test.kt

fun foo(shouldThrow: Boolean) {
    try {
        if (shouldThrow) throw Exception()
    } catch (e: Exception) {
        "OK"
    }
    "OK"
}

fun box() {
    foo(false)
    foo(true)
}

// EXPECTATIONS JVM JVM_IR
// test.kt:15 box
// test.kt:6 foo
// test.kt:7 foo
// test.kt:11 foo
// test.kt:12 foo
// test.kt:16 box
// test.kt:6 foo
// test.kt:7 foo
// test.kt:8 foo
// test.kt:9 foo
// test.kt:11 foo
// test.kt:12 foo
// test.kt:17 box

// EXPECTATIONS JS_IR
// test.kt:15 box
// test.kt:7 foo
// test.kt:12 foo
// test.kt:16 box
// test.kt:7 foo
// test.kt:7 foo
// test.kt:8 foo
// test.kt:8 foo
// test.kt:12 foo
// test.kt:17 box
