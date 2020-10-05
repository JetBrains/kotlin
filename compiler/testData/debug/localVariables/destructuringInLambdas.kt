//FILE: test.kt

data class A(val x: String, val y: Int)

fun foo(a: A, block: (A) -> String): String = block(a)

fun box() {
    foo(A("O", 123)) { (x, y) -> x + y }
}
// IGNORE_BACKEND: JVM_IR
// LOCAL VARIABLES
// TestKt:8:
// A:3: x:java.lang.String="O":java.lang.String, y:int=123:int
// TestKt:8:
// TestKt:5: a:A=A, block:kotlin.jvm.functions.Function1=TestKt$box$1
// TestKt$box$1:8: $dstr$x$y:A=A
// TestKt$box$1.invoke(java.lang.Object)+8:
// TestKt:5: a:A=A, block:kotlin.jvm.functions.Function1=TestKt$box$1
// TestKt:8:
// TestKt:9: