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

// EXPECTATIONS WASM
// test.kt:8 $box: (8, 10, 15, 8)
// test.kt:3 $A.<init>: $<this>:(ref $A)=(ref $A), $x:(ref $kotlin.String)=(ref $kotlin.String), $y:i32=123:i32 (13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 28, 28, 28, 39, 39, 39)
// test.kt:8 $box: (21, 23, 28, 21)
// test.kt:3 $A.<init>: $<this>:(ref $A)=(ref $A), $x:(ref $kotlin.String)=(ref $kotlin.String), $y:i32=877:i32 (13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 28, 28, 28, 39, 39, 39)
// test.kt:8 $box: (21, 4)
// test.kt:5 $foo: $a:(ref $A)=(ref $A), $b:(ref $A)=(ref $A), $block:(ref $box$lambda)=(ref $box$lambda) (55, 61, 64, 55, 55, 55, 55, 55, 55, 55, 55, 55)
// test.kt:8 $box$lambda.invoke: $<this>:(ref $box$lambda)=(ref $box$lambda), $<destruct>:(ref $A)=(ref $A), $x:(ref $kotlin.String)=(ref $kotlin.String), $y:i32=123:i32, $z:(ref $kotlin.String)=(ref $kotlin.String), $w:i32=877:i32 (37, 37, 37, 40, 40, 40, 45, 45, 45, 48, 48, 48, 55, 59, 55, 65, 65, 65, 65, 65, 69, 65, 65, 54, 71)
// test.kt:5 $foo: $a:(ref $A)=(ref $A), $b:(ref $A)=(ref $A), $block:(ref $box$lambda)=(ref $box$lambda) (55, 55, 55, 55, 55, 55, 55, 55, 55, 66)
// test.kt:8 $box: (4)
// test.kt:9 $box: (1)
