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
// A:3: F:x:double, F:y:null, F:z:char
// A:3: F:x:double, F:y:null, F:z:char, LV:x:double, LV:y:java.lang.String, LV:z:char
// A:3: F:x:double, F:y:java.lang.String, F:z:char
// TestKt:11:
// TestKt:5: LV:a:A, LV:block:TestKt$box$1
// TestKt$box$1:14: F:INSTANCE:TestKt$box$1, F:arity:int, LV:$dstr$x$_u24__u24$y:A, LV:$noName_1:java.lang.String, LV:w:int
// A:3: F:x:double, F:y:null, F:z:char
// A:3: F:x:double, F:y:null, F:z:char, LV:x:double, LV:y:java.lang.String, LV:z:char
// A:3: F:x:double, F:y:java.lang.String, F:z:char
// TestKt$box$1:14: F:INSTANCE:TestKt$box$1, F:arity:int, LV:$dstr$x$_u24__u24$y:A, LV:$noName_1:java.lang.String, LV:w:int, LV:x:double, LV:y:char
// TestKt$box$1:15: F:INSTANCE:TestKt$box$1, F:arity:int, LV:$dstr$x$_u24__u24$y:A, LV:$noName_1:java.lang.String, LV:w:int, LV:x:double, LV:y:char, LV:a:double, LV:c:char
// A:3: F:x:double, F:y:null, F:z:char
// A:3: F:x:double, F:y:null, F:z:char, LV:x:double, LV:y:java.lang.String, LV:z:char
// A:3: F:x:double, F:y:java.lang.String, F:z:char
// TestKt$box$1:15: F:INSTANCE:TestKt$box$1, F:arity:int, LV:$dstr$x$_u24__u24$y:A, LV:$noName_1:java.lang.String, LV:w:int, LV:x:double, LV:y:char, LV:a:double, LV:c:char
// TestKt$box$1:17: F:INSTANCE:TestKt$box$1, F:arity:int, LV:$dstr$x$_u24__u24$y:A, LV:$noName_1:java.lang.String, LV:w:int, LV:x:double, LV:y:char, LV:a:double, LV:c:char, LV:_:java.lang.String, LV:d:char
// TestKt:7:
// TestKt$box$1:17: F:INSTANCE:TestKt$box$1, F:arity:int, LV:$dstr$x$_u24__u24$y:A, LV:$noName_1:java.lang.String, LV:w:int, LV:x:double, LV:y:char, LV:a:double, LV:c:char, LV:_:java.lang.String, LV:d:char
// TestKt$box$1:21: F:INSTANCE:TestKt$box$1, F:arity:int, LV:$dstr$x$_u24__u24$y:A, LV:$noName_1:java.lang.String, LV:w:int, LV:x:double, LV:y:char, LV:a:double, LV:c:char, LV:_:java.lang.String, LV:d:char
// TestKt$box$1.invoke(java.lang.Object, java.lang.Object, java.lang.Object)+19: F:INSTANCE:TestKt$box$1, F:arity:int
// TestKt:5: LV:a:A, LV:block:TestKt$box$1
// TestKt:11:
// TestKt:23: