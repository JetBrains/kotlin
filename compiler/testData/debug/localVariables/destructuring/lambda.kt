
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

// EXPECTATIONS WASM
// test.kt:8 $box: (8, 10, 15, 8)
// test.kt:3 $A.<init>: $<this>:(ref $A)=(ref $A), $x:(ref $kotlin.String)=(ref $kotlin.String), $y:i32=123:i32 (13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 28, 28, 28, 39, 39, 39)
// test.kt:8 $box: (8, 4)
// test.kt:5 $foo: $a:(ref $A)=(ref $A), $block:(ref $box$lambda)=(ref $box$lambda) (46, 52, 46, 46, 46, 46, 46, 46, 46, 46, 46)
// test.kt:8 $box$lambda.invoke: $<this>:(ref $box$lambda)=(ref $box$lambda), $<destruct>:(ref $A)=(ref $A), $x:(ref $kotlin.String)=(ref $kotlin.String), $y:i32=123:i32 (24, 24, 24, 27, 27, 27, 33, 37, 37, 37, 37, 37, 37, 33, 38)
// test.kt:5 $foo: $a:(ref $A)=(ref $A), $block:(ref $box$lambda)=(ref $box$lambda) (46, 46, 46, 46, 46, 46, 46, 46, 46, 54)
// test.kt:8 $box: (4)
// test.kt:9 $box: (1)
