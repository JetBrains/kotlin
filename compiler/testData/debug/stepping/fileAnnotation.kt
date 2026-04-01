// WITH_STDLIB
// FILE: test.kt

@file:Suppress("ALL")

fun foo(): String = "O"

class C() {
    val k: String = "K"
}

fun box(): String =
    foo() +
            C().k

// EXPECTATIONS JVM_IR
// test.kt:13 box
// test.kt:6 foo
// test.kt:13 box
// test.kt:14 box
// test.kt:8 <init>
// test.kt:9 <init>
// test.kt:8 <init>
// test.kt:14 box
// test.kt:9 getK
// test.kt:13 box
// test.kt:14 box

// EXPECTATIONS NATIVE
// test.kt:13 box
// test.kt:6 foo
// test.kt:13 box
// test.kt:14 box
// test.kt:8 <init>
// test.kt:9 <init>
// test.kt:8 <init>
// test.kt:14 box
// test.kt:9 <get-k>
// test.kt:14 box
// test.kt:13 box
// test.kt:13 box
// test.kt:14 box

// EXPECTATIONS JS_IR
// test.kt:14 box
// test.kt:6 foo
// test.kt:14 box
// test.kt:9 <init>
// test.kt:8 <init>

// EXPECTATIONS WASM
// test.kt:13 $box (4)
// test.kt:6 $foo (20, 23)
// test.kt:14 $box (12)
// test.kt:9 $C.<init> (20)
// test.kt:8 $C.<init> (9)
// test.kt:14 $box (16)
// test.kt:13 $box (4)
// test.kt:14 $box (17)
