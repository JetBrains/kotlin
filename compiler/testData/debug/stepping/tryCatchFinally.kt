
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
// test.kt:34 $box
// test.kt:6 $foo (8, 8, 8)
// test.kt:26 $mightThrow (8, 8, 8, 22, 22, 16)
// test.kt:27 $mightThrow (1, 1)

// EXPECTATIONS FIR WASM
// test.kt:5 $foo (4, 4, 4, 4, 4, 4)
// test.kt:10 $foo (8, 8, 8, 8, 8, 8)

// EXPECTATIONS ClassicFrontend WASM
// test.kt:5 $foo (4, 4, 4)
// test.kt:10 $foo (8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8)

// EXPECTATIONS WASM
// test.kt:14 $foo (8, 8, 8)
// test.kt:30 $mightThrow2 (8, 8, 22, 22, 16, 8, 22, 22, 16)
// test.kt:31 $mightThrow2
// test.kt:13 $foo (12, 12, 4, 12, 12, 4, 12, 12, 4)
// test.kt:18 $foo (8, 8, 8, 8, 8, 8)
// test.kt:20 $foo (1, 1, 1)
// test.kt:35 $box (13, 4)
// test.kt:36 $box
// test.kt:15 $foo (13, 13)
// test.kt:16 $foo (8, 8, 8, 8, 8, 8, 8, 8)
// test.kt:37 $box (13, 4)
// test.kt:38 $box
// test.kt:7 $foo
// test.kt:8 $foo (8, 8, 8, 8)
// test.kt:39 $box
