//FILE: test.kt

data class A(val x: String, val y: Int)

fun foo(a: A, block: (A) -> String): String = block(a)

fun box() {
    foo(A("O", 123)) { (x, y) -> x + y }
}

// EXPECTATIONS
// test.kt:8 box:
// test.kt:3 <init>: x:java.lang.String="O":java.lang.String, y:int=123:int
// test.kt:8 box:
// test.kt:5 foo: a:A=A, block:kotlin.jvm.functions.Function1=TestKt$box$1

// EXPECTATIONS JVM
// test.kt:8 invoke: $dstr$x$y:A=A
// EXPECTATIONS JVM_IR
// test.kt:8 invoke:

// EXPECTATIONS
// test.kt:5 foo: a:A=A, block:kotlin.jvm.functions.Function1=TestKt$box$1
// test.kt:8 box:
// test.kt:9 box:
