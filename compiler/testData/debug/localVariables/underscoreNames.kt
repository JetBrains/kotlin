
// FILE: test.kt

data class A(val x: Double = 1.0, val y: String = "", val z: Char = '0')

fun foo(a: A, block: (A, String, Int) -> String): String = block(a, "", 1)

val arrayOfA: Array<A> = Array(1) { A() }

fun box() {

    foo(A()) {
        (x, _, y), _, w ->

        val (a, _, c) = A()
        val (_, `_`, d) = A()

        for ((_, q) in arrayOfA) {

        }

        ""
    }
}

// EXPECTATIONS
// test.kt:12 box:
// test.kt:4 <init>: x:double=1.0:double, y:java.lang.String="":java.lang.String, z:char=0:char
// test.kt:12 box:
// test.kt:6 foo: a:A=A, block:kotlin.jvm.functions.Function3=TestKt$box$1

// EXPECTATIONS JVM_IR
// test.kt:13 invoke: w:int=1:int
// test.kt:15 invoke: w:int=1:int, x:double=1.0:double, y:char=0:char
// EXPECTATIONS JVM
// test.kt:15 invoke: $dstr$x$_u24__u24$y:A=A, $noName_1:java.lang.String="":java.lang.String, w:int=1:int

// EXPECTATIONS
// test.kt:4 <init>: x:double=1.0:double, y:java.lang.String="":java.lang.String, z:char=0:char

// EXPECTATIONS JVM
// test.kt:15 invoke: $dstr$x$_u24__u24$y:A=A, $noName_1:java.lang.String="":java.lang.String, w:int=1:int, x:double=1.0:double, y:char=0:char
// test.kt:16 invoke: $dstr$x$_u24__u24$y:A=A, $noName_1:java.lang.String="":java.lang.String, w:int=1:int, x:double=1.0:double, y:char=0:char, a:double=1.0:double, c:char=0:char
// EXPECTATIONS JVM_IR
// test.kt:15 invoke: w:int=1:int, x:double=1.0:double, y:char=0:char
// test.kt:16 invoke: w:int=1:int, x:double=1.0:double, y:char=0:char, a:double=1.0:double, c:char=0:char

// EXPECTATIONS
// test.kt:4 <init>: x:double=1.0:double, y:java.lang.String="":java.lang.String, z:char=0:char

// EXPECTATIONS JVM
// test.kt:16 invoke: $dstr$x$_u24__u24$y:A=A, $noName_1:java.lang.String="":java.lang.String, w:int=1:int, x:double=1.0:double, y:char=0:char, a:double=1.0:double, c:char=0:char
// test.kt:18 invoke: $dstr$x$_u24__u24$y:A=A, $noName_1:java.lang.String="":java.lang.String, w:int=1:int, x:double=1.0:double, y:char=0:char, a:double=1.0:double, c:char=0:char, _:java.lang.String="":java.lang.String, d:char=0:char
// EXPECTATIONS JVM_IR
// test.kt:16 invoke: w:int=1:int, x:double=1.0:double, y:char=0:char, a:double=1.0:double, c:char=0:char
// test.kt:18 invoke: w:int=1:int, x:double=1.0:double, y:char=0:char, a:double=1.0:double, c:char=0:char, _:java.lang.String="":java.lang.String, d:char=0:char

// EXPECTATIONS
// test.kt:8 getArrayOfA:

// EXPECTATIONS JVM
// test.kt:18 invoke: $dstr$x$_u24__u24$y:A=A, $noName_1:java.lang.String="":java.lang.String, w:int=1:int, x:double=1.0:double, y:char=0:char, a:double=1.0:double, c:char=0:char, _:java.lang.String="":java.lang.String, d:char=0:char
// test.kt:22 invoke: $dstr$x$_u24__u24$y:A=A, $noName_1:java.lang.String="":java.lang.String, w:int=1:int, x:double=1.0:double, y:char=0:char, a:double=1.0:double, c:char=0:char, _:java.lang.String="":java.lang.String, d:char=0:char
// EXPECTATIONS JVM_IR
// test.kt:18 invoke: w:int=1:int, x:double=1.0:double, y:char=0:char, a:double=1.0:double, c:char=0:char, _:java.lang.String="":java.lang.String, d:char=0:char
// test.kt:22 invoke: w:int=1:int, x:double=1.0:double, y:char=0:char, a:double=1.0:double, c:char=0:char, _:java.lang.String="":java.lang.String, d:char=0:char

// EXPECTATIONS
// test.kt:6 foo: a:A=A, block:kotlin.jvm.functions.Function3=TestKt$box$1
// test.kt:12 box:
// test.kt:24 box:
