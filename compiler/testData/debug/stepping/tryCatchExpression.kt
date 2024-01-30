
// FILE: test.kt

fun foo() {
    try {
        mightThrow()
    } catch (e: Throwable) {
        return
    }
    
    val t = try {
        mightThrow2()
    } catch (e: Throwable) {
        return
    }

    val x = try {
        mightThrow3()
    } catch (e: Throwable) {
        return
    }
}

var throw1 = false
var throw2 = false
var throw3 = false

fun mightThrow() {
    if (throw1) throw Exception()
}

fun mightThrow2() {
    if (throw2) throw Exception()
}

fun mightThrow3(): Int {
    if (throw3) throw Exception()
    return 42
}

fun box() {
    foo()
    throw3 = true
    foo()
    throw2 = true
    foo()
    throw1 = true
    foo()
}

// EXPECTATIONS JVM_IR
// test.kt:42 box
// test.kt:5 foo
// test.kt:6 foo
// test.kt:29 mightThrow
// test.kt:30 mightThrow
// test.kt:6 foo
// test.kt:11 foo
// test.kt:12 foo
// test.kt:33 mightThrow2
// test.kt:34 mightThrow2
// test.kt:12 foo
// test.kt:11 foo
// test.kt:17 foo
// test.kt:18 foo
// test.kt:37 mightThrow3
// test.kt:38 mightThrow3
// test.kt:18 foo
// test.kt:17 foo
// test.kt:22 foo
// test.kt:43 box
// test.kt:44 box
// test.kt:5 foo
// test.kt:6 foo
// test.kt:29 mightThrow
// test.kt:30 mightThrow
// test.kt:6 foo
// test.kt:11 foo
// test.kt:12 foo
// test.kt:33 mightThrow2
// test.kt:34 mightThrow2
// test.kt:12 foo
// test.kt:11 foo
// test.kt:17 foo
// test.kt:18 foo
// test.kt:37 mightThrow3
// test.kt:19 foo
// test.kt:20 foo
// test.kt:45 box
// test.kt:46 box
// test.kt:5 foo
// test.kt:6 foo
// test.kt:29 mightThrow
// test.kt:30 mightThrow
// test.kt:6 foo
// test.kt:11 foo
// test.kt:12 foo
// test.kt:33 mightThrow2
// test.kt:13 foo
// test.kt:14 foo
// test.kt:47 box
// test.kt:48 box
// test.kt:5 foo
// test.kt:6 foo
// test.kt:29 mightThrow
// test.kt:7 foo
// test.kt:8 foo
// test.kt:49 box

// EXPECTATIONS JS_IR
// test.kt:42 box
// test.kt:6 foo
// test.kt:29 mightThrow
// test.kt:30 mightThrow
// test.kt:12 foo
// test.kt:33 mightThrow2
// test.kt:34 mightThrow2
// test.kt:11 foo
// test.kt:18 foo
// test.kt:37 mightThrow3
// test.kt:38 mightThrow3
// test.kt:17 foo
// test.kt:22 foo
// test.kt:43 box
// test.kt:44 box
// test.kt:6 foo
// test.kt:29 mightThrow
// test.kt:30 mightThrow
// test.kt:12 foo
// test.kt:33 mightThrow2
// test.kt:34 mightThrow2
// test.kt:11 foo
// test.kt:18 foo
// test.kt:37 mightThrow3
// test.kt:37 mightThrow3
// test.kt:19 foo
// test.kt:20 foo
// test.kt:45 box
// test.kt:46 box
// test.kt:6 foo
// test.kt:29 mightThrow
// test.kt:30 mightThrow
// test.kt:12 foo
// test.kt:33 mightThrow2
// test.kt:33 mightThrow2
// test.kt:13 foo
// test.kt:14 foo
// test.kt:47 box
// test.kt:48 box
// test.kt:6 foo
// test.kt:29 mightThrow
// test.kt:29 mightThrow
// test.kt:7 foo
// test.kt:7 foo
// test.kt:8 foo
// test.kt:49 box

// EXPECTATIONS WASM
// test.kt:1 $box
// test.kt:42 $box
// test.kt:6 $foo (8, 8, 8, 8)
// test.kt:29 $mightThrow (8, 8, 8, 8, 22, 22, 16)
// test.kt:30 $mightThrow (1, 1, 1)
// test.kt:12 $foo (8, 8, 8)
// test.kt:33 $mightThrow2 (8, 8, 8, 22, 22, 16)
// test.kt:34 $mightThrow2 (1, 1)
// test.kt:11 $foo (4, 4)
// test.kt:18 $foo (8, 8)
// test.kt:37 $mightThrow3 (8, 8, 22, 22, 16)
// test.kt:38 $mightThrow3 (11, 4)
// test.kt:17 $foo
// test.kt:22 $foo
// test.kt:43 $box (13, 4)
// test.kt:44 $box
// Exceptions.kt:16 $kotlin.Exception.<init> (34, 34, 4, 4, 41, 34, 34, 4, 4, 41, 34, 34, 4, 4, 41)
// Throwable.kt:23 $kotlin.Throwable.<init> (32, 38, 27, 27, 43, 32, 38, 27, 27, 43, 32, 38, 27, 27, 43)
// Throwable.kt:18 $kotlin.Throwable.<init> (28, 62, 28, 62, 28, 62)
// Throwable.kt:25 $kotlin.Throwable.<init> (50, 50, 50)
// ExternalWrapper.kt:226 $kotlin.wasm.internal.jsCheckIsNullOrUndefinedAdapter (18, 8, 32, 33, 18, 8, 32, 33, 18, 8, 32, 33)
// Throwable.kt:27 $kotlin.Throwable.<init> (34, 34, 34)
// Throwable.kt:39 $kotlin.Throwable.<init> (69, 69, 69)
// Throwable.kt:49 $kotlin.Throwable.<init> (1, 1, 1)
// Exceptions.kt:20 $kotlin.Exception.<init> (1, 1, 1)
// test.kt:19 $foo
// test.kt:20 $foo
// test.kt:45 $box (13, 4)
// test.kt:46 $box
// test.kt:13 $foo
// test.kt:14 $foo
// test.kt:47 $box (13, 4)
// test.kt:48 $box
// test.kt:7 $foo
// test.kt:8 $foo
// test.kt:49 $box
