
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
    foo(MyPair("X", "Y")) { (x, y) -> x + y }
}

// EXPECTATIONS ClassicFrontend JVM_IR
// test.kt:16 box:
// test.kt:3 <init>: x:java.lang.String="X":java.lang.String, y:java.lang.String="Y":java.lang.String
// test.kt:16 box:
// test.kt:13 foo: a:MyPair=MyPair, block:kotlin.jvm.functions.Function1=TestKt$box$1
// test.kt:16 invoke:
// test.kt:5 component1:
// test.kt:16 invoke:
// test.kt:9 component2:
// test.kt:16 invoke: x:java.lang.String="O":java.lang.String
// test.kt:13 foo: a:MyPair=MyPair, block:kotlin.jvm.functions.Function1=TestKt$box$1
// test.kt:16 box:
// test.kt:17 box:

// EXPECTATIONS FIR JVM_IR
// test.kt:16 box:
// test.kt:3 <init>: x:java.lang.String="X":java.lang.String, y:java.lang.String="Y":java.lang.String
// test.kt:16 box:
// test.kt:13 foo: a:MyPair=MyPair, block:kotlin.jvm.functions.Function1=TestKt$<lambda>
// test.kt:16 box$lambda$0:
// test.kt:5 component1:
// test.kt:16 box$lambda$0:
// test.kt:9 component2:
// test.kt:16 box$lambda$0: x:java.lang.String="O":java.lang.String
// test.kt:13 foo: a:MyPair=MyPair, block:kotlin.jvm.functions.Function1=TestKt$<lambda>
// test.kt:16 box:
// test.kt:17 box:

// EXPECTATIONS JS_IR
// test.kt:16 box:
// test.kt:3 <init>: x="X":kotlin.String, y="Y":kotlin.String
// test.kt:3 <init>: x="X":kotlin.String, y="Y":kotlin.String
// test.kt:3 <init>: x="X":kotlin.String, y="Y":kotlin.String
// test.kt:16 box:
// test.kt:13 foo: a=MyPair, block=Function1
// test.kt:16 box$lambda:
// test.kt:5 component1:
// test.kt:16 box$lambda: x="O":kotlin.String
// test.kt:9 component2:
// test.kt:16 box$lambda: x="O":kotlin.String, y="K":kotlin.String
// test.kt:17 box:

// EXPECTATIONS WASM
// test.kt:16 $box: (8, 15, 20, 8)
// test.kt:3 $MyPair.<init>: $<this>:(ref $MyPair)=(ref $MyPair), $x:(ref $kotlin.String)=(ref $kotlin.String), $y:(ref $kotlin.String)=(ref $kotlin.String) (13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 28, 28, 28, 42, 42, 42)
// test.kt:16 $box: (8, 4)
// test.kt:13 $foo: $a:(ref $MyPair)=(ref $MyPair), $block:(ref $box$lambda)=(ref $box$lambda) (56, 62, 56, 56, 56, 56, 56, 56, 56, 56, 56)
// test.kt:16 $box$lambda.invoke: $<this>:(ref $box$lambda)=(ref $box$lambda), $<destruct>:(ref $MyPair)=(ref $MyPair), $x:(ref null $kotlin.String)=null, $y:(ref null $kotlin.String)=null (29, 29)
// test.kt:5 $MyPair.component1: $<this>:(ref $MyPair)=(ref $MyPair) (15, 15, 15, 8)
// test.kt:16 $box$lambda.invoke: $<this>:(ref $box$lambda)=(ref $box$lambda), $<destruct>:(ref $MyPair)=(ref $MyPair), $x:(ref $kotlin.String)=(ref $kotlin.String), $y:(ref null $kotlin.String)=null (29, 32, 32)
// test.kt:9 $MyPair.component2: $<this>:(ref $MyPair)=(ref $MyPair) (15, 15, 15, 8)
// test.kt:16 $box$lambda.invoke: $<this>:(ref $box$lambda)=(ref $box$lambda), $<destruct>:(ref $MyPair)=(ref $MyPair), $x:(ref $kotlin.String)=(ref $kotlin.String), $y:(ref $kotlin.String)=(ref $kotlin.String) (32, 38, 42, 38, 43)
// test.kt:13 $foo: $a:(ref $MyPair)=(ref $MyPair), $block:(ref $box$lambda)=(ref $box$lambda) (56, 56, 56, 56, 56, 56, 56, 56, 56, 64)
// test.kt:16 $box: (4)
// test.kt:17 $box: (1)
