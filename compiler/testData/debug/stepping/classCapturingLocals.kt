// WITH_STDLIB

// FILE: test.kt
class A

fun A.foo() {
    val x = baz()
    class B {
        fun bar() {
            println(this@foo)
            println("A.B.bar: $x")
        }
    }
    println("A.foo: $x")
    B().bar()
}

fun baz() = 23

fun box() {
    val a = A()
    a.foo()
}

// EXPECTATIONS JVM_IR
// test.kt:21 box
// test.kt:4 <init>
// test.kt:21 box
// test.kt:22 box
// test.kt:7 foo
// test.kt:18 baz
// test.kt:7 foo
// test.kt:14 foo
// test.kt:15 foo
// test.kt:8 <init>
// test.kt:15 foo
// test.kt:10 bar
// test.kt:11 bar
// test.kt:12 bar
// test.kt:16 foo
// test.kt:23 box

// EXPECTATIONS JS_IR
// test.kt:21 box
// test.kt:4 <init>
// test.kt:22 box
// test.kt:7 foo
// test.kt:18 baz
// test.kt:14 foo
// test.kt:15 foo
// test.kt:8 <init>
// test.kt:15 foo
// test.kt:10 bar
// test.kt:11 bar
// test.kt:12 bar
// test.kt:16 foo
// test.kt:23 box

// EXPECTATIONS WASM
// test.kt:21 $box (12)
// test.kt:4 $A.<init> (7)
// test.kt:22 $box (4, 6)
// test.kt:7 $foo (12)
// test.kt:18 $baz (12, 14)
// test.kt:7 $foo (12)
// test.kt:14 $foo (13, 21, 12, 4)
// test.kt:15 $foo (4)
// test.kt:13 $B.<init> (5)
// test.kt:15 $foo (8)
// test.kt:10 $B.bar (20, 12)
// test.kt:11 $B.bar (21, 31, 20, 12)
// test.kt:12 $B.bar (9)
// test.kt:16 $foo (1)
// test.kt:23 $box (1)

// EXPECTATIONS NATIVE
// test.kt:21 box
// test.kt:4 <init>
// test.kt:21 box
// test.kt:22 box
// test.kt:6 foo
// test.kt:7 foo
// test.kt:18 baz
// test.kt:7 foo
// test.kt:14 foo
// test.kt:14 foo
// test.kt:14 foo
// test.kt:14 foo
// test.kt:15 foo
// test.kt:8 <init>
// test.kt:13 <init>
// test.kt:15 foo
// test.kt:9 bar
// test.kt:10 bar
// test.kt:10 bar
// test.kt:11 bar
// test.kt:11 bar
// test.kt:11 bar
// test.kt:12 bar
// test.kt:16 foo
// test.kt:23 box
