// FILE: test.kt
fun blockFun(blockArg: String.() -> Unit) =
    "OK".blockArg()

fun box() {
    blockFun ({
        println(this)
    })
}

// IGNORE_BACKEND: JVM_IR

// LOCAL VARIABLES
// TestKt:6:
// TestKt:3: blockArg:TestKt$box$1
// TestKt$box$1:7: $this$blockFun:java.lang.String
// TestKt$box$1:8: $this$blockFun:java.lang.String
// TestKt$box$1.invoke(java.lang.Object)+8:
// TestKt$box$1.invoke(java.lang.Object)+11:
// TestKt:3: blockArg:TestKt$box$1
// TestKt:9: