// IGNORE_BACKEND_FIR: JVM_IR

// FILE: test.kt
data class A(val x: String, val y: Int)

fun foo(a: A, block: (A) -> String): String = block(a)

fun box() {
    foo(A("O", 123))
    {
            (
                x
                    ,
                y
            )
        ->
        x + y
    }

    return
}

// EXPECTATIONS JVM
// test.kt:9 box:
// test.kt:4 <init>: x:java.lang.String="O":java.lang.String, y:int=123:int
// test.kt:10 box:
// test.kt:9 box:
// test.kt:6 foo: a:A=A, block:kotlin.jvm.functions.Function1=TestKt$box$1
// test.kt:17 invoke: $dstr$x$y:A=A
// test.kt:6 foo: a:A=A, block:kotlin.jvm.functions.Function1=TestKt$box$1
// test.kt:9 box:
// test.kt:20 box:

// EXPECTATIONS JVM_IR
// test.kt:9 box:
// test.kt:4 <init>: x:java.lang.String="O":java.lang.String, y:int=123:int
// test.kt:9 box:
// test.kt:6 foo: a:A=A, block:kotlin.jvm.functions.Function1=TestKt$box$1
// test.kt:11 invoke:
// test.kt:12 invoke:
// test.kt:11 invoke: x:java.lang.String="O":java.lang.String
// test.kt:14 invoke: x:java.lang.String="O":java.lang.String
// test.kt:17 invoke: x:java.lang.String="O":java.lang.String, y:int=123:int
// test.kt:6 foo: a:A=A, block:kotlin.jvm.functions.Function1=TestKt$box$1
// test.kt:9 box:
// test.kt:20 box:

// EXPECTATIONS JS_IR
// test.kt:9 box:
// test.kt:4 <init>: x="O":kotlin.String, y=123:number
// test.kt:4 <init>: x="O":kotlin.String, y=123:number
// test.kt:4 <init>: x="O":kotlin.String, y=123:number
// test.kt:9 box:
// test.kt:6 foo: a=A, block=Function1
// test.kt:12 box$lambda:
// test.kt:1 component1:
// test.kt:14 box$lambda: x="O":kotlin.String
// test.kt:1 component2:
// test.kt:17 box$lambda: x="O":kotlin.String, y=123:number
// test.kt:20 box:
