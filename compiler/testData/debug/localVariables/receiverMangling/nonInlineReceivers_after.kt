// FILE: test.kt
fun String.foo(count: Int) {
    val x = false

    block {
        this@foo + this@block.toString() + x.toString() + count.toString()
    }
}

fun block(block: Long.() -> Unit) = 5L.block()

fun box() {
    "OK".foo(42)
}

// LOCAL VARIABLES
// TestKt:13:
// TestKt:3: LV:$this$foo:java.lang.String, LV:count:int
// TestKt:5: LV:$this$foo:java.lang.String, LV:count:int, LV:x:boolean
// TestKt:10: LV:block:TestKt$foo$1
// TestKt:8: LV:$this$foo:java.lang.String, LV:count:int, LV:x:boolean
// TestKt:14: