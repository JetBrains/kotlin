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
// A:3: x:double, y:java.lang.String, z:char
// A:3:
// TestKt:11:
// TestKt:5: $dstr$x$_u24__u24$y:A, $noName_1:java.lang.String, w:int
// TestKt$box$1:14:
// A:3: x:double, y:java.lang.String, z:char
// A:3:
// A:3: $dstr$x$_u24__u24$y:A, $noName_1:java.lang.String, w:int, x:double, y:char
// TestKt$box$1:14: $dstr$x$_u24__u24$y:A, $noName_1:java.lang.String, w:int, x:double, y:char, a:double, c:char
// TestKt$box$1:15:
// A:3: x:double, y:java.lang.String, z:char
// A:3:
// A:3: $dstr$x$_u24__u24$y:A, $noName_1:java.lang.String, w:int, x:double, y:char, a:double, c:char
// TestKt$box$1:15: $dstr$x$_u24__u24$y:A, $noName_1:java.lang.String, w:int, x:double, y:char, a:double, c:char, _:java.lang.String, d:char
// TestKt$box$1:17:
// TestKt:7: $dstr$x$_u24__u24$y:A, $noName_1:java.lang.String, w:int, x:double, y:char, a:double, c:char, _:java.lang.String, d:char
// TestKt$box$1:17: $dstr$x$_u24__u24$y:A, $noName_1:java.lang.String, w:int, x:double, y:char, a:double, c:char, _:java.lang.String, d:char
// TestKt$box$1:21:
// TestKt$box$1.invoke(java.lang.Object, java.lang.Object, java.lang.Object)+19: a:A, block:TestKt$box$1
// TestKt:5: a:A, block:TestKt$box$1
// TestKt:11:
// TestKt:23: