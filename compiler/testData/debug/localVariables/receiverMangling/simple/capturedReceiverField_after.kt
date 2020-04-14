// !LANGUAGE: +NewCapturedReceiverFieldNamingConvention
// FILE: test.kt
fun String.foo(count: Int) {
    block {
        block {
            this@foo
        }
    }
}

fun block(block: Long.() -> Unit) = 5L.block()

fun box() {
    "OK".foo(42)
}

// IGNORE_BACKEND: JVM_IR
// LOCAL VARIABLES
// TestKt:14:
// TestKt:4: LV:$this$foo:java.lang.String, LV:count:int
// TestKt:11: LV:block:TestKt$foo$1
// TestKt:11: LV:block:TestKt$foo$1$1
// TestKt$foo$1$1:6: F:this$0:TestKt$foo$1, F:arity:int, LV:$this$block:long
// TestKt$foo$1$1:7: F:this$0:TestKt$foo$1, F:arity:int, LV:$this$block:long
// TestKt$foo$1$1.invoke(java.lang.Object)+11: F:this$0:TestKt$foo$1, F:arity:int
// TestKt$foo$1$1.invoke(java.lang.Object)+14: F:this$0:TestKt$foo$1, F:arity:int
// TestKt:11: LV:block:TestKt$foo$1$1
// TestKt$foo$1:8: F:$this_foo:java.lang.String, F:arity:int, LV:$this$block:long
// TestKt$foo$1.invoke(java.lang.Object)+11: F:$this_foo:java.lang.String, F:arity:int
// TestKt$foo$1.invoke(java.lang.Object)+14: F:$this_foo:java.lang.String, F:arity:int
// TestKt:11: LV:block:TestKt$foo$1
// TestKt:9: LV:$this$foo:java.lang.String, LV:count:int
// TestKt:15: