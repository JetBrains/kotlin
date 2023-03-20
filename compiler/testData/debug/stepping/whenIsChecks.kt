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

// EXPECTATIONS JVM JVM_IR
// test.kt:15 box
// test.kt:4 foo
// test.kt:5 foo
// test.kt:6 foo
// test.kt:12 foo
// test.kt:16 box
// test.kt:4 foo
// test.kt:5 foo
// test.kt:7 foo
// test.kt:8 foo
// test.kt:12 foo
// test.kt:17 box
// test.kt:4 foo
// test.kt:5 foo
// test.kt:7 foo
// test.kt:10 foo
// test.kt:12 foo
// test.kt:18 box

// EXPECTATIONS JS_IR
// test.kt:15 box
// test.kt:4 foo
// test.kt:5 foo
// test.kt:12 foo
// test.kt:16 box
// test.kt:4 foo
// test.kt:5 foo
// test.kt:12 foo
// test.kt:17 box
// test.kt:4 foo
// test.kt:5 foo
// test.kt:12 foo
// test.kt:18 box
