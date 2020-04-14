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
// TestKt:3: $this$foo:java.lang.String, count:int
// TestKt:5: $this$foo:java.lang.String, count:int, x:boolean
// TestKt:10: block:TestKt$foo$1
// TestKt:8: $this$foo:java.lang.String, count:int, x:boolean
// TestKt:14: