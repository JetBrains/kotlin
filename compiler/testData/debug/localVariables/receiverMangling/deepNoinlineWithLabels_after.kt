// !LANGUAGE: +NewCapturedReceiverFieldNamingConvention
// FILE: test.kt
fun String.foo(count: Int) {
    val x = false

    block b1@ {
        val y = false
        block b2@ {
            val z = true
            block b3@ {
                this@foo + this@b1 + this@b2 + this@b3 + x + y + z + count
            }
        }
    }
}

fun block(block: Long.() -> Unit) = 5L.block()

fun box() {
    "OK".foo(42)
}

// IGNORE_BACKEND: JVM_IR

// LOCAL VARIABLES
// TestKt:20:
// TestKt:4: LV:$this$foo:java.lang.String, LV:count:int
// TestKt:6: LV:$this$foo:java.lang.String, LV:count:int, LV:x:boolean
// TestKt:17: LV:block:TestKt$foo$1
// TestKt$foo$1:7: F:$this_foo:java.lang.String, F:$x:boolean, F:$count:int, F:arity:int, LV:$this$b1:long
// TestKt$foo$1:8: F:$this_foo:java.lang.String, F:$x:boolean, F:$count:int, F:arity:int, LV:$this$b1:long, LV:y:boolean
// TestKt:17: LV:block:TestKt$foo$1$1
// TestKt$foo$1$1:9: F:this$0:TestKt$foo$1, F:$this_b1:long, F:$y:boolean, F:arity:int, LV:$this$b2:long
// TestKt$foo$1$1:10: F:this$0:TestKt$foo$1, F:$this_b1:long, F:$y:boolean, F:arity:int, LV:$this$b2:long, LV:z:boolean
// TestKt:17: LV:block:TestKt$foo$1$1$1
// TestKt$foo$1$1:13: F:this$0:TestKt$foo$1, F:$this_b1:long, F:$y:boolean, F:arity:int, LV:$this$b2:long
// TestKt$foo$1$1.invoke(java.lang.Object)+11: F:this$0:TestKt$foo$1, F:$this_b1:long, F:$y:boolean, F:arity:int
// TestKt$foo$1$1.invoke(java.lang.Object)+14: F:this$0:TestKt$foo$1, F:$this_b1:long, F:$y:boolean, F:arity:int
// TestKt:17: LV:block:TestKt$foo$1$1
// TestKt$foo$1:14: F:$this_foo:java.lang.String, F:$x:boolean, F:$count:int, F:arity:int, LV:$this$b1:long
// TestKt$foo$1.invoke(java.lang.Object)+11: F:$this_foo:java.lang.String, F:$x:boolean, F:$count:int, F:arity:int
// TestKt$foo$1.invoke(java.lang.Object)+14: F:$this_foo:java.lang.String, F:$x:boolean, F:$count:int, F:arity:int
// TestKt:17: LV:block:TestKt$foo$1
// TestKt:15: LV:$this$foo:java.lang.String, LV:count:int, LV:x:boolean
// TestKt:21: