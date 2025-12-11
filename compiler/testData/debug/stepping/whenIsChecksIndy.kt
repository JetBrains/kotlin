// IGNORE_DEXING
// JDK_KIND: FULL_JDK_21
// JVM_TARGET: 21
// TARGET_BACKEND: JVM
// WHEN_EXPRESSIONS: INDY

// FILE: test.kt

fun foo(x: Any) {
    when (x) {
        is Float ->
            1
        is Double ->
            2
        else ->
            3
    }
}

fun box() {
    foo(1.2f)
    foo(1.2)
    foo(1)
}

// EXPECTATIONS JVM_IR
// test.kt:21 box
// test.kt:10 foo
// test.kt:12 foo
// test.kt:18 foo
// test.kt:22 box
// test.kt:10 foo
// test.kt:14 foo
// test.kt:18 foo
// test.kt:23 box
// test.kt:10 foo
// test.kt:16 foo
// test.kt:18 foo
// test.kt:24 box