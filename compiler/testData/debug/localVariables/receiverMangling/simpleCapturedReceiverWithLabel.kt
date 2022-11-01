

// FILE: test.kt
fun blockFun(blockArg: String.() -> Unit) =
    "OK".blockArg()

fun box() {
    blockFun label@{
        this
    }
}

// EXPECTATIONS JVM JVM_IR
// test.kt:8 box:
// test.kt:5 blockFun: blockArg:kotlin.jvm.functions.Function1=TestKt$box$1
// test.kt:9 invoke: $this$label:java.lang.String="OK":java.lang.String
// test.kt:10 invoke: $this$label:java.lang.String="OK":java.lang.String
// test.kt:5 blockFun: blockArg:kotlin.jvm.functions.Function1=TestKt$box$1
// test.kt:11 box:

// EXPECTATIONS JS_IR
// test.kt:8 box:
// test.kt:5 blockFun: blockArg=Function1
// test.kt:10 box$lambda: $this$label="OK":kotlin.String
// test.kt:11 box:
