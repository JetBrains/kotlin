
// FILE: test.kt

class A {
    fun foo() = this
    inline fun bar() = this
}

fun box() {
    val a = A()
    a
     .foo()

    a
     .bar()
}

// EXPECTATIONS JVM_IR
// test.kt:10 box
// test.kt:4 <init>
// test.kt:10 box
// test.kt:11 box
// test.kt:12 box
// test.kt:5 foo
// test.kt:12 box
// test.kt:14 box
// test.kt:15 box
// test.kt:6 box
// test.kt:16 box

// EXPECTATIONS JS_IR
// test.kt:10 box
// test.kt:4 <init>
// test.kt:12 box
// test.kt:5 foo
// test.kt:16 box

// EXPECTATIONS WASM
// test.kt:1 $box
// test.kt:10 $box (12, 12)
// test.kt:7 $A.<init>
// test.kt:11 $box
// test.kt:12 $box (6, 6)
// test.kt:5 $A.foo (16, 20)
// test.kt:15 $box (6, 6)
// test.kt:14 $box
// test.kt:6 $box (23, 27)
// test.kt:16 $box
