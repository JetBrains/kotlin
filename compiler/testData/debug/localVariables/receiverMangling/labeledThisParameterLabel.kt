

// FILE: test.kt
fun blockFun(blockArg: String.() -> Unit) =
    "OK".blockArg()

fun box() {
    blockFun {
        println(this)
    }
}

// LOCAL VARIABLES
// test.kt:8 box:
// test.kt:5 blockFun: blockArg:kotlin.jvm.functions.Function1=TestKt$box$1
// test.kt:9 invoke: $this$blockFun:java.lang.String="OK":java.lang.String
// test.kt:10 invoke: $this$blockFun:java.lang.String="OK":java.lang.String
// test.kt:5 blockFun: blockArg:kotlin.jvm.functions.Function1=TestKt$box$1
// test.kt:11 box: