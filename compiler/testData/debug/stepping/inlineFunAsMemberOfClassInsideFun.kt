
// FILE: test.kt
fun foo(): String {
    class A {
        inline fun bar() = "OK"
    }

    return A()
        .bar()
}

fun box(): String {
    return foo()
}

// EXPECTATIONS JVM_IR
// test.kt:13 box
// test.kt:8 foo
// test.kt:4 <init>
// test.kt:8 foo
// test.kt:9 foo
// test.kt:5 foo
// test.kt:8 foo
// test.kt:13 box

// EXPECTATIONS NATIVE
// test.kt:13 box
// test.kt:3 foo
// test.kt:8 foo
// test.kt:4 <init>
// test.kt:6 <init>
// test.kt:8 foo
// test.kt:9 foo
// test.kt:5 foo
// test.kt:9 foo
// test.kt:10 foo
// test.kt:13 box
// test.kt:14 box

// EXPECTATIONS JS_IR
// test.kt:13 box
// test.kt:8 foo
// test.kt:4 <init>
// test.kt:8 foo

// EXPECTATIONS WASM
// test.kt:13 $box (11)
// test.kt:8 $foo (11)
// test.kt:6 $A.<init> (5)
// test.kt:8 $foo (11)
// test.kt:9 $foo (9)
// test.kt:5 $foo (27, 31)
// test.kt:8 $foo (4)
// test.kt:13 $box (4)
