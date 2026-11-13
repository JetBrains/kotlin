// FILE: test.kt

fun foo(x: Any) {
    when (x) {
        is B ->
            1
        is A ->
            2
        else ->
            3
    }
}

fun box() {
    foo(A())
    foo(B())
    foo(Any())
}

open class A

open class B : A()

// EXPECTATIONS JVM
// test.kt:15 box
// test.kt:20 <init>
// test.kt:15 box
// test.kt:4 foo
// test.kt:5 foo
// test.kt:7 foo
// test.kt:8 foo
// test.kt:12 foo
// test.kt:16 box
// test.kt:22 <init>
// test.kt:20 <init>
// test.kt:22 <init>
// test.kt:16 box
// test.kt:4 foo
// test.kt:5 foo
// test.kt:6 foo
// test.kt:12 foo
// test.kt:17 box
// test.kt:4 foo
// test.kt:5 foo
// test.kt:7 foo
// test.kt:10 foo
// test.kt:12 foo
// test.kt:18 box

// EXPECTATIONS JS_IR
// test.kt:15 box
// test.kt:20 <init>
// test.kt:15 box
// test.kt:5 foo
// test.kt:7 foo
// test.kt:8 foo
// test.kt:12 foo
// test.kt:16 box
// test.kt:22 <init>
// test.kt:20 <init>
// test.kt:22 <init>
// test.kt:16 box
// test.kt:5 foo
// test.kt:6 foo
// test.kt:8 foo
// test.kt:12 foo
// test.kt:17 box
// test.kt:5 foo
// test.kt:7 foo
// test.kt:8 foo
// test.kt:12 foo
// test.kt:18 box

// EXPECTATIONS WASM_JS
// test.kt:15 $box (8)
// test.kt:20 $A.<init> (12)
// test.kt:15 $box (4)
// test.kt:4 $foo (10)
// test.kt:5 $foo (8)
// test.kt:7 $foo (8)
// test.kt:8 $foo (12)
// test.kt:12 $foo (1)
// test.kt:16 $box (8)
// test.kt:22 $B.<init> (0)
// test.kt:20 $A.<init> (12)
// test.kt:22 $B.<init> (0, 18)
// test.kt:16 $box (4)
// test.kt:4 $foo (10)
// test.kt:5 $foo (8)
// test.kt:6 $foo (12)
// test.kt:12 $foo (1)
// test.kt:17 $box (8, 4)
// test.kt:4 $foo (10)
// test.kt:5 $foo (8)
// test.kt:7 $foo (8)
// test.kt:10 $foo (12)
// test.kt:12 $foo (1)
// test.kt:18 $box (1)

// EXPECTATIONS NATIVE
// test.kt:15 box
// test.kt:20 <init>
// test.kt:15 box
// test.kt:3 foo
// test.kt:4 foo
// test.kt:5 foo
// test.kt:7 foo
// test.kt:8 foo
// test.kt:12 foo
// test.kt:15 box
// test.kt:16 box
// test.kt:22 <init>
// test.kt:20 <init>
// test.kt:22 <init>
// test.kt:16 box
// test.kt:3 foo
// test.kt:4 foo
// test.kt:5 foo
// test.kt:6 foo
// test.kt:12 foo
// test.kt:16 box
// test.kt:17 box
// test.kt:3 foo
// test.kt:4 foo
// test.kt:5 foo
// test.kt:7 foo
// test.kt:10 foo
// test.kt:12 foo
// test.kt:18 box
