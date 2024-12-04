// IGNORE_BACKEND: JVM_IR
// FILE: test.kt
data class A(val x: String, val y: Int)

fun foo(a: A, b: A, block: (A, A) -> String): String = block(a, b)

fun box() {
    foo(A("O", 123), A("K", 877)) { (x, y), (z, w) -> (x + z) + (y + w) }
}

// EXPECTATIONS JVM_IR
// test.kt:9 box:
// test.kt:4 <init>: x:java.lang.String="O":java.lang.String, y:int=123:int
// test.kt:9 box:
// test.kt:4 <init>: x:java.lang.String="K":java.lang.String, y:int=877:int
// test.kt:9 box:
// test.kt:6 foo: a:A=A, b:A=A, block:kotlin.jvm.functions.Function2=TestKt$box$1
// test.kt:9 invoke: x:java.lang.String="O":java.lang.String, y:int=123:int, z:java.lang.String="K":java.lang.String, w:int=877:int
// test.kt:6 foo: a:A=A, b:A=A, block:kotlin.jvm.functions.Function2=TestKt$box$1
// test.kt:9 box:
// test.kt:10 box:

// EXPECTATIONS JS_IR
// test.kt:8 box:
// test.kt:3 <init>: x="O":kotlin.String, y=123:number
// test.kt:3 <init>: x="O":kotlin.String, y=123:number
// test.kt:3 <init>: x="O":kotlin.String, y=123:number
// test.kt:8 box:
// test.kt:3 <init>: x="K":kotlin.String, y=877:number
// test.kt:3 <init>: x="K":kotlin.String, y=877:number
// test.kt:3 <init>: x="K":kotlin.String, y=877:number
// test.kt:8 box:
// test.kt:5 foo: a=A, b=A, block=Function2
// test.kt:8 box$lambda:
// test.kt:1 component1:
// test.kt:8 box$lambda: x="O":kotlin.String
// test.kt:1 component2:
// test.kt:8 box$lambda: x="O":kotlin.String, y=123:number
// test.kt:1 component1:
// test.kt:8 box$lambda: x="O":kotlin.String, y=123:number, z="K":kotlin.String
// test.kt:1 component2:
// test.kt:8 box$lambda: x="O":kotlin.String, y=123:number, z="K":kotlin.String, w=877:number
// test.kt:9 box:
