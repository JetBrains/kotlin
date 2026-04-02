

// FILE: test.kt
fun blockFun(blockArg: String.() -> Unit) =
    "OK".blockArg()

fun box() {
    blockFun {
        this
    }
}

// EXPECTATIONS JVM_IR
// test.kt:8 box:
// test.kt:5 blockFun: blockArg:kotlin.jvm.functions.Function1=TestKt$<lambda>
// test.kt:9 box$lambda$0: $this$blockFun:java.lang.String="OK":java.lang.String
// test.kt:10 box$lambda$0: $this$blockFun:java.lang.String="OK":java.lang.String
// test.kt:5 blockFun: blockArg:kotlin.jvm.functions.Function1=TestKt$<lambda>
// test.kt:11 box:

// EXPECTATIONS JS_IR
// test.kt:8 box:
// test.kt:5 blockFun: blockArg=Function1
// test.kt:10 box$lambda: $this$blockFun="OK":kotlin.String
// test.kt:11 box:
