
// FILE: test.kt

class E1: Exception()

class E2: Exception()

class A()

fun foo(): A =
    try {
        throw E1()
    } catch (e: E2) {
        A()
    } catch (e: Exception) {
        A()
    }

fun box(): String {
    foo()
    return "OK"
}

// EXPECTATIONS JVM_IR
// test.kt:20 box
// test.kt:11 foo
// test.kt:12 foo
// test.kt:4 <init>
// test.kt:12 foo
// test.kt:15 foo
// test.kt:16 foo
// test.kt:8 <init>
// test.kt:16 foo
// test.kt:17 foo
// test.kt:20 box
// test.kt:21 box

// EXPECTATIONS NATIVE
// test.kt:20 box
// test.kt:10 foo
// test.kt:12 foo
// test.kt:4 <init>
// test.kt:4 <init>
// test.kt:12 foo
// test.kt:17 foo
// test.kt:15 foo
// test.kt:16 foo
// test.kt:8 <init>
// test.kt:16 foo
// test.kt:17 foo
// test.kt:20 box
// test.kt:21 box
// test.kt:22 box

// EXPECTATIONS JS_IR
// test.kt:20 box
// test.kt:12 foo
// test.kt:4 <init>
// test.kt:4 <init>
// test.kt:15 foo
// test.kt:16 foo
// test.kt:8 <init>
// test.kt:17 foo
// test.kt:21 box

// EXPECTATIONS WASM
// test.kt:20 $box (4)
// test.kt:12 $foo (14)
// test.kt:4 $E1.<init> (0, 21)
// test.kt:12 $foo (8)
// test.kt:14 $foo (8)
// test.kt:16 $foo (8)
// test.kt:8 $A.<init> (9)
// test.kt:16 $foo (8)
// test.kt:17 $foo (5)
// test.kt:20 $box (4)
// test.kt:21 $box (11, 4)
