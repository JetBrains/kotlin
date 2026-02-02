

// FILE: test.kt
class MyPair(val x: String, val y: String) {
    operator fun component1(): String {
        return "O"
    }

    operator fun component2(): String {
        return "K"
    }
}

fun foo(a: MyPair, block: (MyPair) -> String): String = block(a)

fun box() {
    foo(MyPair("X", "Y"))
    {
            (
                x
                    ,
                y
            )
        ->
        x + y
    }
}

// EXPECTATIONS ClassicFrontend JVM_IR
// test.kt:17 box:
// test.kt:4 <init>: x:java.lang.String="X":java.lang.String, y:java.lang.String="Y":java.lang.String
// test.kt:17 box:
// test.kt:14 foo: a:MyPair=MyPair, block:kotlin.jvm.functions.Function1=TestKt$box$1
// test.kt:19 invoke:
// test.kt:6 component1:
// test.kt:20 invoke:
// test.kt:19 invoke: x:java.lang.String="O":java.lang.String
// test.kt:10 component2:
// test.kt:22 invoke: x:java.lang.String="O":java.lang.String
// test.kt:25 invoke: x:java.lang.String="O":java.lang.String, y:java.lang.String="K":java.lang.String
// test.kt:14 foo: a:MyPair=MyPair, block:kotlin.jvm.functions.Function1=TestKt$box$1
// test.kt:17 box:
// test.kt:27 box:

// EXPECTATIONS FIR JVM_IR
// test.kt:17 box:
// test.kt:4 <init>: x:java.lang.String="X":java.lang.String, y:java.lang.String="Y":java.lang.String
// test.kt:17 box:
// test.kt:14 foo: a:MyPair=MyPair, block:kotlin.jvm.functions.Function1=TestKt$<lambda>
// test.kt:25 box$lambda$0:
// test.kt:6 component1:
// test.kt:25 box$lambda$0:
// test.kt:10 component2:
// test.kt:25 box$lambda$0: x:java.lang.String="O":java.lang.String
// test.kt:14 foo: a:MyPair=MyPair, block:kotlin.jvm.functions.Function1=TestKt$<lambda>
// test.kt:17 box:
// test.kt:27 box:

// EXPECTATIONS JS_IR
// test.kt:17 box:
// test.kt:4 <init>: x="X":kotlin.String, y="Y":kotlin.String
// test.kt:4 <init>: x="X":kotlin.String, y="Y":kotlin.String
// test.kt:4 <init>: x="X":kotlin.String, y="Y":kotlin.String
// test.kt:17 box:
// test.kt:14 foo: a=MyPair, block=Function1
// test.kt:20 box$lambda:
// test.kt:6 component1:
// test.kt:22 box$lambda: x="O":kotlin.String
// test.kt:10 component2:
// test.kt:25 box$lambda: x="O":kotlin.String, y="K":kotlin.String
// test.kt:27 box:

// EXPECTATIONS WASM
// test.kt:17 $box: (8, 15, 20, 8)
// test.kt:4 $MyPair.<init>: $<this>:(ref $MyPair)=(ref $MyPair), $x:(ref $kotlin.String)=(ref $kotlin.String), $y:(ref $kotlin.String)=(ref $kotlin.String) (13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 28, 28, 28, 42, 42, 42)
// test.kt:17 $box: (8, 4)
// test.kt:14 $foo: $a:(ref $MyPair)=(ref $MyPair), $block:(ref $box$lambda)=(ref $box$lambda) (56, 62, 56, 56, 56, 56, 56, 56, 56, 56, 56)
// test.kt:20 $box$lambda.invoke: $<this>:(ref $box$lambda)=(ref $box$lambda), $<destruct>:(ref $MyPair)=(ref $MyPair), $x:(ref null $kotlin.String)=null, $y:(ref null $kotlin.String)=null (16, 16)
// test.kt:6 $MyPair.component1: $<this>:(ref $MyPair)=(ref $MyPair) (15, 15, 15, 8)
// test.kt:20 $box$lambda.invoke: $<this>:(ref $box$lambda)=(ref $box$lambda), $<destruct>:(ref $MyPair)=(ref $MyPair), $x:(ref null $kotlin.String)=null, $y:(ref null $kotlin.String)=null (16)
// test.kt:22 $box$lambda.invoke: $<this>:(ref $box$lambda)=(ref $box$lambda), $<destruct>:(ref $MyPair)=(ref $MyPair), $x:(ref $kotlin.String)=(ref $kotlin.String), $y:(ref null $kotlin.String)=null (16, 16)
// test.kt:10 $MyPair.component2: $<this>:(ref $MyPair)=(ref $MyPair) (15, 15, 15, 8)
// test.kt:22 $box$lambda.invoke: $<this>:(ref $box$lambda)=(ref $box$lambda), $<destruct>:(ref $MyPair)=(ref $MyPair), $x:(ref $kotlin.String)=(ref $kotlin.String), $y:(ref null $kotlin.String)=null (16)
// test.kt:25 $box$lambda.invoke: $<this>:(ref $box$lambda)=(ref $box$lambda), $<destruct>:(ref $MyPair)=(ref $MyPair), $x:(ref $kotlin.String)=(ref $kotlin.String), $y:(ref $kotlin.String)=(ref $kotlin.String) (8, 12, 8, 13)
// test.kt:14 $foo: $a:(ref $MyPair)=(ref $MyPair), $block:(ref $box$lambda)=(ref $box$lambda) (56, 56, 56, 56, 56, 56, 56, 56, 56, 64)
// test.kt:17 $box: (4)
// test.kt:27 $box: (1)
