// FILE: test.kt

fun foo() {
    try {
        mightThrow()
    } catch (e: Throwable) {
        "OK"
    } finally {
        "FINALLY"
    }
    
    val t = try {
        mightThrow2()
    } catch (e: Throwable) {
        "OK2"
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
    throw1 = true
    foo()
}

// EXPECTATIONS JVM JVM_IR
// test.kt:33 box
// test.kt:4 foo
// test.kt:5 foo
// test.kt:25 mightThrow
// test.kt:26 mightThrow
// test.kt:9 foo
// test.kt:10 foo
// test.kt:12 foo
// test.kt:13 foo
// test.kt:29 mightThrow2
// test.kt:30 mightThrow2
// test.kt:13 foo
// test.kt:17 foo
// test.kt:18 foo
// test.kt:12 foo
// test.kt:19 foo
// test.kt:34 box
// test.kt:35 box
// test.kt:4 foo
// test.kt:5 foo
// test.kt:25 mightThrow
// test.kt:26 mightThrow
// test.kt:9 foo
// test.kt:10 foo
// test.kt:12 foo
// test.kt:13 foo
// test.kt:29 mightThrow2
// test.kt:14 foo
// test.kt:15 foo
// test.kt:17 foo
// test.kt:18 foo
// test.kt:12 foo
// test.kt:19 foo
// test.kt:36 box
// test.kt:37 box
// test.kt:4 foo
// test.kt:5 foo
// test.kt:25 mightThrow
// test.kt:6 foo
// test.kt:7 foo
// test.kt:9 foo
// test.kt:10 foo
// test.kt:12 foo
// test.kt:13 foo
// test.kt:29 mightThrow2
// test.kt:14 foo
// test.kt:15 foo
// test.kt:17 foo
// test.kt:18 foo
// test.kt:12 foo
// test.kt:19 foo
// test.kt:38 box

// EXPECTATIONS JS_IR
// test.kt:33 box
// test.kt:5 foo
// test.kt:25 mightThrow
// test.kt:26 mightThrow
// test.kt:13 foo
// test.kt:29 mightThrow2
// test.kt:30 mightThrow2
// test.kt:12 foo
// test.kt:19 foo
// test.kt:34 box
// test.kt:35 box
// test.kt:5 foo
// test.kt:25 mightThrow
// test.kt:26 mightThrow
// test.kt:13 foo
// test.kt:29 mightThrow2
// test.kt:29 mightThrow2
// test.kt:14 foo
// test.kt:15 foo
// test.kt:12 foo
// test.kt:19 foo
// test.kt:36 box
// test.kt:37 box
// test.kt:5 foo
// test.kt:25 mightThrow
// test.kt:25 mightThrow
// test.kt:6 foo
// test.kt:6 foo
// test.kt:13 foo
// test.kt:29 mightThrow2
// test.kt:29 mightThrow2
// test.kt:14 foo
// test.kt:15 foo
// test.kt:12 foo
// test.kt:19 foo
// test.kt:38 box
