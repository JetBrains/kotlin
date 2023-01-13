// IGNORE_BACKEND: JVM_IR
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

// EXPECTATIONS JVM
// test.kt:16 box:
// test.kt:3 <init>: x:java.lang.String="X":java.lang.String, y:java.lang.String="Y":java.lang.String
// test.kt:16 box:
// test.kt:13 foo: a:MyPair=MyPair, block:kotlin.jvm.functions.Function1=TestKt$box$1
// test.kt:16 invoke: $dstr$x$y:MyPair=MyPair
// test.kt:5 component1:
// test.kt:16 invoke: $dstr$x$y:MyPair=MyPair
// test.kt:9 component2:
// test.kt:16 invoke: $dstr$x$y:MyPair=MyPair
// test.kt:13 foo: a:MyPair=MyPair, block:kotlin.jvm.functions.Function1=TestKt$box$1
// test.kt:16 box:
// test.kt:17 box:

// EXPECTATIONS JVM_IR
// test.kt:17 box:
// test.kt:4 <init>: x:java.lang.String="X":java.lang.String, y:java.lang.String="Y":java.lang.String
// test.kt:17 box:
// test.kt:14 foo: a:MyPair=MyPair, block:kotlin.jvm.functions.Function1=TestKt$box$1
// test.kt:17 invoke:
// test.kt:6 component1:
// test.kt:17 invoke:
// test.kt:10 component2:
// test.kt:17 invoke: x:java.lang.String="O":java.lang.String, y:java.lang.String="K":java.lang.String
// test.kt:14 foo: a:MyPair=MyPair, block:kotlin.jvm.functions.Function1=TestKt$box$1
// test.kt:17 box:
// test.kt:18 box:

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