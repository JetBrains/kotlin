//FILE: test.kt

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
// IGNORE_BACKEND: JVM_IR
// LOCAL VARIABLES
// test.kt:11 box:
// test.kt:3 <init>: x:double=1.0:double, y:java.lang.String="":java.lang.String, z:char=0:char
// test.kt:11 box:
// test.kt:5 foo: a:A=A, block:kotlin.jvm.functions.Function3=TestKt$box$1
// test.kt:14 invoke: $dstr$x$_u24__u24$y:A=A, $noName_1:java.lang.String="":java.lang.String, w:int=1:int
// test.kt:3 <init>: x:double=1.0:double, y:java.lang.String="":java.lang.String, z:char=0:char
// test.kt:14 invoke: $dstr$x$_u24__u24$y:A=A, $noName_1:java.lang.String="":java.lang.String, w:int=1:int, x:double=1.0:double, y:char=0:char
// test.kt:15 invoke: $dstr$x$_u24__u24$y:A=A, $noName_1:java.lang.String="":java.lang.String, w:int=1:int, x:double=1.0:double, y:char=0:char, a:double=1.0:double, c:char=0:char
// test.kt:3 <init>: x:double=1.0:double, y:java.lang.String="":java.lang.String, z:char=0:char
// test.kt:15 invoke: $dstr$x$_u24__u24$y:A=A, $noName_1:java.lang.String="":java.lang.String, w:int=1:int, x:double=1.0:double, y:char=0:char, a:double=1.0:double, c:char=0:char
// test.kt:17 invoke: $dstr$x$_u24__u24$y:A=A, $noName_1:java.lang.String="":java.lang.String, w:int=1:int, x:double=1.0:double, y:char=0:char, a:double=1.0:double, c:char=0:char, _:java.lang.String="":java.lang.String, d:char=0:char
// test.kt:7 getArrayOfA:
// test.kt:17 invoke: $dstr$x$_u24__u24$y:A=A, $noName_1:java.lang.String="":java.lang.String, w:int=1:int, x:double=1.0:double, y:char=0:char, a:double=1.0:double, c:char=0:char, _:java.lang.String="":java.lang.String, d:char=0:char
// test.kt:21 invoke: $dstr$x$_u24__u24$y:A=A, $noName_1:java.lang.String="":java.lang.String, w:int=1:int, x:double=1.0:double, y:char=0:char, a:double=1.0:double, c:char=0:char, _:java.lang.String="":java.lang.String, d:char=0:char
// test.kt:5 foo: a:A=A, block:kotlin.jvm.functions.Function3=TestKt$box$1
// test.kt:11 box:
// test.kt:23 box: