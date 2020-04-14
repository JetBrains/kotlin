// FILE: test.kt
fun String.foo(count: Int) {
    val x: Boolean = false

    block {
        this@foo + this@block.toString() + x.toString() + count.toString()
    }
}

inline fun block(block: Long.() -> Unit) = 5L.block()

fun box() {
    "OK".foo(42)
}

// IGNORE_BACKEND: JVM_IR

// LOCAL VARIABLES
// TestKt:13:
// TestKt:3: LV:$this$foo:java.lang.String, LV:count:int
// TestKt:5: LV:$this$foo:java.lang.String, LV:count:int, LV:x:boolean
// TestKt:10: LV:$this$foo:java.lang.String, LV:count:int, LV:x:boolean, LV:$i$f$block:int
// TestKt:6: LV:$this$foo:java.lang.String, LV:count:int, LV:x:boolean, LV:$i$f$block:int, LV:$this$block:long, LV:$i$a$-block-TestKt$foo$1:int
// TestKt:7: LV:$this$foo:java.lang.String, LV:count:int, LV:x:boolean, LV:$i$f$block:int, LV:$this$block:long, LV:$i$a$-block-TestKt$foo$1:int
// TestKt:8: LV:$this$foo:java.lang.String, LV:count:int, LV:x:boolean
// TestKt:14: