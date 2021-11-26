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

// EXPECTATIONS
// test.kt:8 box
// test.kt:4 foo
// test.kt:8 box
// test.kt:11 box
