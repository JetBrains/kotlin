

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

// TODO(KT-86460): the FQN of `Function1` sometimes is `Function1` and sometimes `kotlin.test.Function1`, so specifying a correct option isn't possible here; re-enable this EXPECTATIONS WASM block after KT-86460 is solved by replacing this line with // EXPECTATIONS WASM
// test.kt:8 $box: (13, 4)
// test.kt:5 $blockFun: $blockArg:(ref $kotlin.test.Function1)=(ref $kotlin.test.Function1) (9, 4, 4, 4, 9, 9, 9, 9, 9, 9, 9, 9, 9)
// test.kt:8 $box$lambda.invoke: $$this$blockFun:(ref $kotlin.String)=(ref $kotlin.String) (13, 13, 13)
// test.kt:9 $box$lambda.invoke: $$this$blockFun:(ref $kotlin.String)=(ref $kotlin.String) (8, 8)
// test.kt:8 $box$lambda.invoke: $$this$blockFun:(ref $kotlin.String)=(ref $kotlin.String) (13)
// test.kt:5 $blockFun: $blockArg:(ref $kotlin.test.Function1)=(ref $kotlin.test.Function1) (9, 9, 9, 9, 9, 9, 9, 9, 9, 19)
// test.kt:11 $box: (1)
