

// FILE: test.kt
fun blockFun(blockArg: String.() -> Unit) =
    "OK".blockArg()

fun box() {
    blockFun label@{
        this
    }
}

// EXPECTATIONS ClassicFrontend JVM_IR
// test.kt:8 box:
// test.kt:5 blockFun: blockArg:kotlin.jvm.functions.Function1=TestKt$box$1
// test.kt:9 invoke: $this$label:java.lang.String="OK":java.lang.String
// test.kt:10 invoke: $this$label:java.lang.String="OK":java.lang.String
// test.kt:5 blockFun: blockArg:kotlin.jvm.functions.Function1=TestKt$box$1
// test.kt:11 box:

// EXPECTATIONS FIR JVM_IR
// test.kt:8 box:
// test.kt:5 blockFun: blockArg:kotlin.jvm.functions.Function1=TestKt$<lambda>
// test.kt:9 box$lambda$0: $this$label:java.lang.String="OK":java.lang.String
// test.kt:10 box$lambda$0: $this$label:java.lang.String="OK":java.lang.String
// test.kt:5 blockFun: blockArg:kotlin.jvm.functions.Function1=TestKt$<lambda>
// test.kt:11 box:

// EXPECTATIONS JS_IR
// test.kt:8 box:
// test.kt:5 blockFun: blockArg=Function1
// test.kt:10 box$lambda: $this$label="OK":kotlin.String
// test.kt:11 box:

// EXPECTATIONS WASM
// test.kt:8 $box: (4)
// test.kt:5 $blockFun: $blockArg:(ref $box$lambda)=(ref $box$lambda) (9, 4, 4, 4, 9, 9, 9, 9, 9, 9, 9, 9, 9)
// test.kt:9 $box$lambda.invoke: $<this>:(ref $box$lambda)=(ref $box$lambda), $$this$label:(ref $kotlin.String)=(ref $kotlin.String) (8, 8, 12)
// test.kt:10 $box$lambda.invoke: $<this>:(ref $box$lambda)=(ref $box$lambda), $$this$label:(ref $kotlin.String)=(ref $kotlin.String) (5)
// test.kt:5 $blockFun: $blockArg:(ref $box$lambda)=(ref $box$lambda) (9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 19)
// test.kt:11 $box: (1)
