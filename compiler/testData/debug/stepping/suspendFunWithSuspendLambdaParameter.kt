
// WITH_STDLIB
// FILE: test.kt
suspend fun foo(block: suspend Long.() -> String): String {
    return 1L.block()
}

suspend fun box() {
    foo {
        "OK"
    }
}

// This is the actual unfortunate stepping behavior in intellij.
// There is a class exclusion filter for anything in the kotlin package.
// That means that we never step into the lambda as that is only
// called via code in the kotlin package.

// EXPECTATIONS JVM JVM_IR
// test.kt:9 box
// test.kt:5 foo
// test.kt:9 box
// test.kt:12 box

// EXPECTATIONS JS_IR
// test.kt:9 doResume
// test.kt:9 box$slambda
// test.kt:9 doResume
// test.kt:6 foo
// test.kt:5 foo
// test.kt:10 doResume
// test.kt:12 doResume
