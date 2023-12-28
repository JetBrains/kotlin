

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
// test.kt:1 $box
// test.kt:15 $box (4, 4, 8)
// test.kt:5 $A.<init>
// test.kt:12 $A.<init>
// test.kt:10 $A.foo (13, 13, 13, 13)
// String.kt:141 $kotlin.stringLiteral (17, 28, 17)
// Array.kt:59 $kotlin.Array.get (19, 26, 34, 8)
// ThrowHelpers.kt:29 $kotlin.wasm.internal.rangeCheck (6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19)
// ThrowHelpers.kt:30 $kotlin.wasm.internal.rangeCheck (1, 1)
// Array.kt:60 $kotlin.Array.get (15, 27, 23, 8)
// String.kt:142 $kotlin.stringLiteral
// String.kt:146 $kotlin.stringLiteral (47, 61, 16, 4)
// String.kt:147 $kotlin.stringLiteral (20, 20, 20, 20, 27, 33, 41, 20, 4)
// String.kt:148 $kotlin.stringLiteral (4, 15, 25, 4)
// Array.kt:74 $kotlin.Array.set (19, 26, 34, 8)
// Array.kt:75 $kotlin.Array.set (8, 20, 27, 16)
// Array.kt:76 $kotlin.Array.set
// String.kt:149 $kotlin.stringLiteral (11, 4)
// test.kt:11 $A.foo
// test.kt:16 $box
