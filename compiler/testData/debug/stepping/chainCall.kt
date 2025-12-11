
// FILE: test.kt

class A {
    fun foo() = this
    inline fun bar() = this
}

fun box() {
    val a = A()
    a.foo()
        .foo()

    a.foo()
        .bar()

    a.bar()
        .bar()
        .bar()
        .bar()
        .bar()
}

// EXPECTATIONS JVM_IR
// test.kt:10 box
// test.kt:4 <init>
// test.kt:10 box
// test.kt:11 box
// test.kt:5 foo
// test.kt:12 box
// test.kt:5 foo
// test.kt:12 box
// test.kt:14 box
// test.kt:5 foo
// test.kt:14 box
// test.kt:15 box
// test.kt:6 box
// test.kt:17 box
// test.kt:6 box
// test.kt:18 box
// test.kt:6 box
// test.kt:19 box
// test.kt:6 box
// test.kt:20 box
// test.kt:6 box
// test.kt:21 box
// test.kt:6 box
// test.kt:22 box

// EXPECTATIONS NATIVE
// test.kt:10 box
// test.kt:4 <init>
// test.kt:7 <init>
// test.kt:10 box
// test.kt:11 box
// test.kt:5 foo
// test.kt:11 box
// test.kt:12 box
// test.kt:5 foo
// test.kt:12 box
// test.kt:14 box
// test.kt:5 foo
// test.kt:14 box
// test.kt:15 box
// test.kt:6 box
// test.kt:17 box
// test.kt:6 box
// test.kt:17 box
// test.kt:18 box
// test.kt:6 box
// test.kt:18 box
// test.kt:19 box
// test.kt:6 box
// test.kt:19 box
// test.kt:20 box
// test.kt:6 box
// test.kt:20 box
// test.kt:21 box
// test.kt:6 box
// test.kt:22 box

// EXPECTATIONS JS_IR
// test.kt:10 box
// test.kt:4 <init>
// test.kt:11 box
// test.kt:5 foo
// test.kt:12 box
// test.kt:5 foo
// test.kt:14 box
// test.kt:5 foo
// test.kt:22 box

// EXPECTATIONS WASM
// test.kt:10 $box (12)
// test.kt:7 $A.<init> (1)
// test.kt:11 $box (4, 6)
// test.kt:5 $A.foo (16, 20)
// test.kt:12 $box (9)
// test.kt:5 $A.foo (16, 20)
// test.kt:12 $box (9)
// test.kt:14 $box (4, 6)
// test.kt:5 $A.foo (16, 20)
// test.kt:14 $box (6)
// test.kt:15 $box (9)
// test.kt:6 $box (23, 27)
// test.kt:17 $box (4, 6)
// test.kt:6 $box (23, 27)
// test.kt:18 $box (9)
// test.kt:6 $box (23, 27)
// test.kt:19 $box (9)
// test.kt:6 $box (23, 27)
// test.kt:20 $box (9)
// test.kt:6 $box (23, 27)
// test.kt:21 $box (9)
// test.kt:6 $box (23, 27)
// test.kt:22 $box (1)
