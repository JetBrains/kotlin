
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

// EXPECTATIONS JVM_IR
// test.kt:34 box
// test.kt:5 foo
// test.kt:6 foo
// test.kt:26 mightThrow
// test.kt:27 mightThrow
// test.kt:10 foo
// test.kt:11 foo
// test.kt:13 foo
// test.kt:14 foo
// test.kt:30 mightThrow2
// test.kt:31 mightThrow2
// test.kt:14 foo
// test.kt:18 foo
// test.kt:19 foo
// test.kt:13 foo
// test.kt:20 foo
// test.kt:35 box
// test.kt:36 box
// test.kt:5 foo
// test.kt:6 foo
// test.kt:26 mightThrow
// test.kt:27 mightThrow
// test.kt:10 foo
// test.kt:11 foo
// test.kt:13 foo
// test.kt:14 foo
// test.kt:30 mightThrow2
// test.kt:15 foo
// test.kt:16 foo
// test.kt:18 foo
// test.kt:19 foo
// test.kt:13 foo
// test.kt:20 foo
// test.kt:37 box
// test.kt:38 box
// test.kt:5 foo
// test.kt:6 foo
// test.kt:26 mightThrow
// test.kt:7 foo
// test.kt:8 foo
// test.kt:10 foo
// test.kt:11 foo
// test.kt:13 foo
// test.kt:14 foo
// test.kt:30 mightThrow2
// test.kt:15 foo
// test.kt:16 foo
// test.kt:18 foo
// test.kt:19 foo
// test.kt:13 foo
// test.kt:20 foo
// test.kt:39 box

// EXPECTATIONS JS_IR
// test.kt:34 box
// test.kt:6 foo
// test.kt:26 mightThrow
// test.kt:27 mightThrow
// test.kt:14 foo
// test.kt:30 mightThrow2
// test.kt:31 mightThrow2
// test.kt:13 foo
// test.kt:20 foo
// test.kt:35 box
// test.kt:36 box
// test.kt:6 foo
// test.kt:26 mightThrow
// test.kt:27 mightThrow
// test.kt:14 foo
// test.kt:30 mightThrow2
// test.kt:30 mightThrow2
// test.kt:15 foo
// test.kt:16 foo
// test.kt:13 foo
// test.kt:20 foo
// test.kt:37 box
// test.kt:38 box
// test.kt:6 foo
// test.kt:26 mightThrow
// test.kt:26 mightThrow
// test.kt:7 foo
// test.kt:7 foo
// test.kt:14 foo
// test.kt:30 mightThrow2
// test.kt:30 mightThrow2
// test.kt:15 foo
// test.kt:16 foo
// test.kt:13 foo
// test.kt:20 foo
// test.kt:39 box

// EXPECTATIONS WASM
// test.kt:33 $box (10)
// test.kt:34 $box (4)
// test.kt:4 $foo (10)
// test.kt:6 $foo (8)
// test.kt:25 $mightThrow (17)
// test.kt:26 $mightThrow (8, 16, 22)
// test.kt:27 $mightThrow (1)
// test.kt:5 $foo (4)
// test.kt:10 $foo (8)
// test.kt:14 $foo (8)
// test.kt:29 $mightThrow2 (18)
// test.kt:30 $mightThrow2 (8, 16, 22)
// test.kt:31 $mightThrow2 (1)
// test.kt:13 $foo (12)
// test.kt:18 $foo (8)
// test.kt:20 $foo (1)
// test.kt:35 $box (13, 4)
// test.kt:36 $box (4)
// test.kt:15 $foo (13)
// test.kt:16 $foo (8)
// test.kt:37 $box (13, 4)
// test.kt:38 $box (4)
// test.kt:7 $foo (13)
// test.kt:8 $foo (8)
// test.kt:39 $box (1)
