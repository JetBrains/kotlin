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
// TestKt:3: $this$foo:java.lang.String, count:int
// TestKt:5: $this$foo:java.lang.String, count:int, x:boolean
// TestKt:10: $this$foo:java.lang.String, count:int, x:boolean, $i$f$block:int
// TestKt:6: $this$foo:java.lang.String, count:int, x:boolean, $i$f$block:int, $this$block:long, $i$a$-block-TestKt$foo$1:int
// TestKt:7: $this$foo:java.lang.String, count:int, x:boolean, $i$f$block:int, $this$block:long, $i$a$-block-TestKt$foo$1:int
// TestKt:8: $this$foo:java.lang.String, count:int, x:boolean
// TestKt:14: