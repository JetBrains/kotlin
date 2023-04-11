// FILE: test.kt

fun foo(x: Int) {
    when (x) {
        21 -> foo(42)
        42 -> foo(63)
        else -> 1
    }
    
    val t = when (x) {
        21 -> foo(42)
        42 -> foo(63)
        else -> 1
    }
}

fun box() {
    foo(21)
}

// EXPECTATIONS JVM JVM_IR
// test.kt:18 box
// test.kt:4 foo
// test.kt:5 foo
// test.kt:4 foo
// test.kt:6 foo
// test.kt:4 foo
// test.kt:7 foo
// test.kt:10 foo
// test.kt:13 foo
// test.kt:10 foo
// test.kt:15 foo
// test.kt:6 foo
// test.kt:10 foo
// test.kt:12 foo
// test.kt:4 foo
// test.kt:7 foo
// test.kt:10 foo
// test.kt:13 foo
// test.kt:10 foo
// test.kt:15 foo
// test.kt:12 foo
// test.kt:10 foo
// test.kt:15 foo
// test.kt:5 foo
// test.kt:10 foo
// test.kt:11 foo
// test.kt:4 foo
// test.kt:6 foo
// test.kt:4 foo
// test.kt:7 foo
// test.kt:10 foo
// test.kt:13 foo
// test.kt:10 foo
// test.kt:15 foo
// test.kt:6 foo
// test.kt:10 foo
// test.kt:12 foo
// test.kt:4 foo
// test.kt:7 foo
// test.kt:10 foo
// test.kt:13 foo
// test.kt:10 foo
// test.kt:15 foo
// test.kt:12 foo
// test.kt:10 foo
// test.kt:15 foo
// test.kt:11 foo
// test.kt:10 foo
// test.kt:15 foo
// test.kt:19 box

// EXPECTATIONS JS_IR
// test.kt:18 box
// test.kt:4 foo
// test.kt:5 foo
// test.kt:4 foo
// test.kt:6 foo
// test.kt:4 foo
// test.kt:10 foo
// test.kt:13 foo
// test.kt:10 foo
// test.kt:15 foo
// test.kt:10 foo
// test.kt:12 foo
// test.kt:4 foo
// test.kt:10 foo
// test.kt:13 foo
// test.kt:10 foo
// test.kt:15 foo
// test.kt:10 foo
// test.kt:15 foo
// test.kt:10 foo
// test.kt:11 foo
// test.kt:4 foo
// test.kt:6 foo
// test.kt:4 foo
// test.kt:10 foo
// test.kt:13 foo
// test.kt:10 foo
// test.kt:15 foo
// test.kt:10 foo
// test.kt:12 foo
// test.kt:4 foo
// test.kt:10 foo
// test.kt:13 foo
// test.kt:10 foo
// test.kt:15 foo
// test.kt:10 foo
// test.kt:15 foo
// test.kt:10 foo
// test.kt:15 foo
// test.kt:19 box
