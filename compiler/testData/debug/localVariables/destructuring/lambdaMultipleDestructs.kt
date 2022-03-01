

// FILE: test.kt
data class A(val x: String, val y: Int)

fun foo(a: A, b: A, block: (A, A) -> String): String = block(a, b)

fun box() {
    foo(A("O", 123), A("K", 877)) { (x, y), (z, w) -> (x + z) + (y + w) }
}

// EXPECTATIONS

// EXPECTATIONS JVM
// test.kt:9 box:
// test.kt:4 <init>: x:java.lang.String="O":java.lang.String, y:int=123:int
// test.kt:9 box:
// test.kt:4 <init>: x:java.lang.String="K":java.lang.String, y:int=877:int
// test.kt:9 box:
// test.kt:6 foo: a:A=A, b:A=A, block:kotlin.jvm.functions.Function2=TestKt$box$1
// test.kt:9 invoke: $dstr$x$y:A=A, $dstr$z$w:A=A
// test.kt:6 foo: a:A=A, b:A=A, block:kotlin.jvm.functions.Function2=TestKt$box$1
// test.kt:9 box:
// test.kt:10 box:

// EXPECTATIONS JVM_IR
// test.kt:9 box:
// test.kt:4 <init>: x:java.lang.String="O":java.lang.String, y:int=123:int
// test.kt:9 box:
// test.kt:4 <init>: x:java.lang.String="K":java.lang.String, y:int=877:int
// test.kt:9 box:
// test.kt:6 foo: a:A=A, b:A=A, block:kotlin.jvm.functions.Function2=TestKt$box$1
// test.kt:9 invoke:
// test.kt:6 foo: a:A=A, b:A=A, block:kotlin.jvm.functions.Function2=TestKt$box$1
// test.kt:9 box:
// test.kt:10 box: