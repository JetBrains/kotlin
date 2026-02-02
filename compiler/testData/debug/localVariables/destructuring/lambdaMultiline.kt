

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

// EXPECTATIONS ClassicFrontend JVM_IR
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

// EXPECTATIONS FIR JVM_IR
// test.kt:9 box:
// test.kt:4 <init>: x:java.lang.String="O":java.lang.String, y:int=123:int
// test.kt:9 box:
// test.kt:6 foo: a:A=A, block:kotlin.jvm.functions.Function1=TestKt$<lambda>
// test.kt:17 box$lambda$0:
// test.kt:6 foo: a:A=A, block:kotlin.jvm.functions.Function1=TestKt$<lambda>
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

// EXPECTATIONS WASM
// test.kt:9 $box: (8, 10, 15, 8)
// test.kt:4 $A.<init>: $<this>:(ref $A)=(ref $A), $x:(ref $kotlin.String)=(ref $kotlin.String), $y:i32=123:i32 (13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 28, 28, 28, 39, 39, 39)
// test.kt:9 $box: (8, 4)
// test.kt:6 $foo: $a:(ref $A)=(ref $A), $block:(ref $box$lambda)=(ref $box$lambda) (46, 52, 46, 46, 46, 46, 46, 46, 46, 46, 46)
// test.kt:12 $box$lambda.invoke: $<this>:(ref $box$lambda)=(ref $box$lambda), $<destruct>:(ref $A)=(ref $A), $x:(ref null $kotlin.String)=null, $y:i32=0:i32 (16, 16, 16)
// test.kt:14 $box$lambda.invoke: $<this>:(ref $box$lambda)=(ref $box$lambda), $<destruct>:(ref $A)=(ref $A), $x:(ref $kotlin.String)=(ref $kotlin.String), $y:i32=0:i32 (16, 16, 16)
// test.kt:17 $box$lambda.invoke: $<this>:(ref $box$lambda)=(ref $box$lambda), $<destruct>:(ref $A)=(ref $A), $x:(ref $kotlin.String)=(ref $kotlin.String), $y:i32=123:i32 (8, 12, 12, 12, 12, 12, 12, 8, 13)
// test.kt:6 $foo: $a:(ref $A)=(ref $A), $block:(ref $box$lambda)=(ref $box$lambda) (46, 46, 46, 46, 46, 46, 46, 46, 46, 54)
// test.kt:9 $box: (4)
// test.kt:20 $box: (4)
