// FILE: test.kt
fun blockFun(blockArg: String.() -> Unit) =
    "OK".blockArg()

fun box() {
    blockFun {
        println(this)
    }
}

// LOCAL VARIABLES
// TestKt:6:
// TestKt:3: LV:blockArg:TestKt$box$1
// TestKt$box$1:7: F:INSTANCE:TestKt$box$1, F:arity:int, LV:$this$blockFun:java.lang.String
// TestKt$box$1:8: F:INSTANCE:TestKt$box$1, F:arity:int, LV:$this$blockFun:java.lang.String
// TestKt$box$1.invoke(java.lang.Object)+8: F:INSTANCE:TestKt$box$1, F:arity:int
// TestKt$box$1.invoke(java.lang.Object)+11: F:INSTANCE:TestKt$box$1, F:arity:int
// TestKt:3: LV:blockArg:TestKt$box$1
// TestKt:9: