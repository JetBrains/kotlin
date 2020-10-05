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
// TestKt:11:
// A:3:
// A:3: x:double=1.0:double, y:java.lang.String="":java.lang.String, z:char=0:char
// A:3:
// TestKt:11:
// TestKt:5: a:A=A, block:kotlin.jvm.functions.Function3=TestKt$box$1
// TestKt$box$1:14: $dstr$x$_u24__u24$y:A=A, $noName_1:java.lang.String="":java.lang.String, w:int=1:int
// A:3:
// A:3: x:double=1.0:double, y:java.lang.String="":java.lang.String, z:char=0:char
// A:3:
// TestKt$box$1:14: $dstr$x$_u24__u24$y:A=A, $noName_1:java.lang.String="":java.lang.String, w:int=1:int, x:double=1.0:double, y:char=0:char
// TestKt$box$1:15: $dstr$x$_u24__u24$y:A=A, $noName_1:java.lang.String="":java.lang.String, w:int=1:int, x:double=1.0:double, y:char=0:char, a:double=1.0:double, c:char=0:char
// A:3:
// A:3: x:double=1.0:double, y:java.lang.String="":java.lang.String, z:char=0:char
// A:3:
// TestKt$box$1:15: $dstr$x$_u24__u24$y:A=A, $noName_1:java.lang.String="":java.lang.String, w:int=1:int, x:double=1.0:double, y:char=0:char, a:double=1.0:double, c:char=0:char
// TestKt$box$1:17: $dstr$x$_u24__u24$y:A=A, $noName_1:java.lang.String="":java.lang.String, w:int=1:int, x:double=1.0:double, y:char=0:char, a:double=1.0:double, c:char=0:char, _:java.lang.String="":java.lang.String, d:char=0:char
// TestKt:7:
// TestKt$box$1:17: $dstr$x$_u24__u24$y:A=A, $noName_1:java.lang.String="":java.lang.String, w:int=1:int, x:double=1.0:double, y:char=0:char, a:double=1.0:double, c:char=0:char, _:java.lang.String="":java.lang.String, d:char=0:char
// TestKt$box$1:21: $dstr$x$_u24__u24$y:A=A, $noName_1:java.lang.String="":java.lang.String, w:int=1:int, x:double=1.0:double, y:char=0:char, a:double=1.0:double, c:char=0:char, _:java.lang.String="":java.lang.String, d:char=0:char
// TestKt$box$1.invoke(java.lang.Object, java.lang.Object, java.lang.Object)+19:
// TestKt:5: a:A=A, block:kotlin.jvm.functions.Function3=TestKt$box$1
// TestKt:11:
// TestKt:23: