// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR
// FILE: test.kt
data class A(val x: String, val y: Int)

fun foo(a: A, block: (A) -> String): String = block(a)

fun box() {
    foo(A("O", 123)) { (x, y) -> x + y }
}

// EXPECTATIONS
// EXPECTATIONS JVM
// test.kt:9 box:
// test.kt:4 <init>: x:java.lang.String="O":java.lang.String, y:int=123:int
// test.kt:9 box:
// test.kt:6 foo: a:A=A, block:kotlin.jvm.functions.Function1=TestKt$box$1
// test.kt:9 invoke: $dstr$x$y:A=A
// test.kt:6 foo: a:A=A, block:kotlin.jvm.functions.Function1=TestKt$box$1
// test.kt:9 box:
// test.kt:10 box:

// EXPECTATIONS JVM_IR
// test.kt:9 box:
// test.kt:4 <init>: x:java.lang.String="O":java.lang.String, y:int=123:int
// test.kt:9 box:
// test.kt:6 foo: a:A=A, block:kotlin.jvm.functions.Function1=TestKt$box$1
// test.kt:9 invoke: x:java.lang.String="O":java.lang.String, y:int=123:int
// test.kt:6 foo: a:A=A, block:kotlin.jvm.functions.Function1=TestKt$box$1
// test.kt:9 box:
// test.kt:10 box: