// FILE: test.kt

fun foo() {
    try {
        mightThrow()
    } finally {
        "FINALLY"
    }
    
    val t = try {
        mightThrow2()
    } finally {
        "FINALLY2"
    }
}

var throw1 = false
var throw2 = false

fun mightThrow() {
    if (throw1) throw Exception()
}

fun mightThrow2() {
    if (throw2) throw Exception()
}

fun box() {
    foo()
    throw2 = true
    foo()
    // Never gets here.
    throw1 = true
    foo()
}

// The JVM backend steps back to line 11 when leaving the
// `mightThrow2` call. The JVM_IR backend does not. The
// JVM_IR behavior is consistent with what happens for the
// try-finally where the value is discarded which seems good.

// EXPECTATIONS JVM JVM_IR
// test.kt:29 box
// test.kt:4 foo
// test.kt:5 foo
// test.kt:21 mightThrow
// test.kt:22 mightThrow
// test.kt:7 foo
// test.kt:8 foo
// test.kt:10 foo
// test.kt:11 foo
// test.kt:25 mightThrow2
// test.kt:26 mightThrow2
// EXPECTATIONS JVM
// test.kt:11 foo
// EXPECTATIONS JVM JVM_IR
// test.kt:13 foo
// test.kt:14 foo
// test.kt:10 foo
// test.kt:15 foo
// test.kt:30 box
// test.kt:31 box
// test.kt:4 foo
// test.kt:5 foo
// test.kt:21 mightThrow
// test.kt:22 mightThrow
// test.kt:7 foo
// test.kt:8 foo
// test.kt:10 foo
// test.kt:11 foo
// test.kt:25 mightThrow2
// EXPECTATIONS JVM
// test.kt:14 foo
// test.kt:10 foo
// EXPECTATIONS JVM_IR
// test.kt:13 foo

// EXPECTATIONS JS_IR
// test.kt:29 box
// test.kt:5 foo
// test.kt:21 mightThrow
// test.kt:22 mightThrow
// test.kt:11 foo
// test.kt:25 mightThrow2
// test.kt:26 mightThrow2
// test.kt:10 foo
// test.kt:15 foo
// test.kt:30 box
// test.kt:31 box
// test.kt:5 foo
// test.kt:21 mightThrow
// test.kt:22 mightThrow
// test.kt:11 foo
// test.kt:25 mightThrow2
// test.kt:25 mightThrow2