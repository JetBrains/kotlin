

// FILE: test.kt
class A {
    val a = 1
    fun bar() = 2
    fun foo() {
        3

            .toString()
    }
}

fun box() {
    A().foo()
}

// EXPECTATIONS JVM_IR
// test.kt:15 box
// test.kt:4 <init>
// test.kt:5 <init>
// test.kt:4 <init>
// test.kt:15 box
// test.kt:10 foo
// test.kt:11 foo
// test.kt:16 box

// EXPECTATIONS JS_IR
// test.kt:15 box
// test.kt:5 <init>
// test.kt:4 <init>
// test.kt:15 box
// test.kt:11 foo
// test.kt:16 box

// EXPECTATIONS WASM
// test.kt:15 $box (4, 8)
// test.kt:5 $A.<init> (12, 12, 12)
// test.kt:12 $A.<init>
// test.kt:10 $A.foo (13, 13, 13, 13, 13)
// test.kt:11 $A.foo
// test.kt:16 $box
