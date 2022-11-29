// WITH_STDLIB
// FILE: test.kt
suspend fun foo(block: Long.() -> String): String {
    return 1L.block()
}

suspend fun box() {
    foo {
        "OK"
    }
}

// EXPECTATIONS JVM JVM_IR
// test.kt:8 box
// test.kt:4 foo
// test.kt:9 invoke
// test.kt:4 foo
// test.kt:8 box
// test.kt:11 box

// EXPECTATIONS JS_IR
// test.kt:8 doResume
// test.kt:4 foo
// test.kt:4 foo
// test.kt:9 box$lambda
// test.kt:11 doResume
