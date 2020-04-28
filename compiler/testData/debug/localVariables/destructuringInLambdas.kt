//FILE: test.kt

data class A(val x: String, val y: Int)

fun foo(a: A, block: (A) -> String): String = block(a)

fun box() {
    foo(A("O", 123)) { (x, y) -> x + y }
}
// IGNORE_BACKEND: JVM_IR
// LOCAL VARIABLES
// TestKt:8:
// A:3: F:x:null, F:y:int, LV:x:java.lang.String, LV:y:int
// TestKt:8:
// TestKt:5: LV:a:A, LV:block:TestKt$box$1
// TestKt$box$1:8: F:INSTANCE:TestKt$box$1, F:arity:int, LV:$dstr$x$y:A
// TestKt$box$1.invoke(java.lang.Object)+8: F:INSTANCE:TestKt$box$1, F:arity:int
// TestKt:5: LV:a:A, LV:block:TestKt$box$1
// TestKt:8:
// TestKt:9: