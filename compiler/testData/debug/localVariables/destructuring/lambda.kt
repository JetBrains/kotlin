
// FILE: test.kt
data class A(val x: String, val y: Int)

fun foo(a: A, block: (A) -> String): String = block(a)

fun box() {
    foo(A("O", 123)) { (x, y) -> x + y }
}

// EXPECTATIONS ClassicFrontend JVM_IR
// test.kt:8 box:
// test.kt:3 <init>: x:java.lang.String="O":java.lang.String, y:int=123:int
// test.kt:8 box:
// test.kt:5 foo: a:A=A, block:kotlin.jvm.functions.Function1=TestKt$box$1
// test.kt:8 invoke:
// test.kt:5 foo: a:A=A, block:kotlin.jvm.functions.Function1=TestKt$box$1
// test.kt:8 box:
// test.kt:9 box:

// EXPECTATIONS FIR JVM_IR
// test.kt:8 box:
// test.kt:3 <init>: x:java.lang.String="O":java.lang.String, y:int=123:int
// test.kt:8 box:
// test.kt:5 foo: a:A=A, block:kotlin.jvm.functions.Function1=TestKt$<lambda>
// test.kt:8 box$lambda$0:
// test.kt:5 foo: a:A=A, block:kotlin.jvm.functions.Function1=TestKt$<lambda>
// test.kt:8 box:
// test.kt:9 box:

// EXPECTATIONS JS_IR
// test.kt:8 box:
// test.kt:3 <init>: x="O":kotlin.String, y=123:number
// test.kt:3 <init>: x="O":kotlin.String, y=123:number
// test.kt:3 <init>: x="O":kotlin.String, y=123:number
// test.kt:8 box:
// test.kt:5 foo: a=A, block=Function1
// test.kt:8 box$lambda:
// test.kt:1 component1:
// test.kt:8 box$lambda: x="O":kotlin.String
// test.kt:1 component2:
// test.kt:8 box$lambda: x="O":kotlin.String, y=123:number
// test.kt:9 box:
