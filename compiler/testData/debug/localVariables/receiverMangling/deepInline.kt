// FILE: test.kt
fun String.foo(count: Int) {
    val x = false

    block {
        val y = false
        block {
            val z = true
            block {
                this@foo + this@block.toString() + x + y + z + count
            }
        }
    }
}

inline fun block(block: Long.() -> Unit) = 5L.block()

fun box() {
    "OK".foo(42)
}

// IGNORE_BACKEND: JVM_IR

// LOCAL VARIABLES
// TestKt:19:
// TestKt:3: $this$foo:java.lang.String, count:int
// TestKt:5: $this$foo:java.lang.String, count:int, x:boolean
// TestKt:16: $this$foo:java.lang.String, count:int, x:boolean, $i$f$block:int
// TestKt:6: $this$foo:java.lang.String, count:int, x:boolean, $i$f$block:int, $this$block:long, $i$a$-block-TestKt$foo$1:int
// TestKt:7: $this$foo:java.lang.String, count:int, x:boolean, $i$f$block:int, $this$block:long, $i$a$-block-TestKt$foo$1:int, y:boolean
// TestKt:16: $this$foo:java.lang.String, count:int, x:boolean, $this$block:long, $i$a$-block-TestKt$foo$1:int, y:boolean, $i$f$block:int
// TestKt:8: $this$foo:java.lang.String, count:int, x:boolean, $i$a$-block-TestKt$foo$1:int, y:boolean, $i$f$block:int, $this$block:long, $i$a$-block-TestKt$foo$1$1:int
// TestKt:9: $this$foo:java.lang.String, count:int, x:boolean, $i$a$-block-TestKt$foo$1:int, y:boolean, $i$f$block:int, $this$block:long, $i$a$-block-TestKt$foo$1$1:int, z:boolean
// TestKt:16: $this$foo:java.lang.String, count:int, x:boolean, $i$a$-block-TestKt$foo$1:int, y:boolean, $this$block:long, $i$a$-block-TestKt$foo$1$1:int, z:boolean, $i$f$block:int
// TestKt:10: $this$foo:java.lang.String, count:int, x:boolean, $i$a$-block-TestKt$foo$1:int, y:boolean, $i$a$-block-TestKt$foo$1$1:int, z:boolean, $i$f$block:int, $this$block:long, $i$a$-block-TestKt$foo$1$1$1:int
// TestKt:11: $this$foo:java.lang.String, count:int, x:boolean, $i$a$-block-TestKt$foo$1:int, y:boolean, $i$a$-block-TestKt$foo$1$1:int, z:boolean, $i$f$block:int, $this$block:long, $i$a$-block-TestKt$foo$1$1$1:int
// TestKt:12: $this$foo:java.lang.String, count:int, x:boolean, $i$a$-block-TestKt$foo$1:int, y:boolean, $i$f$block:int, $this$block:long, $i$a$-block-TestKt$foo$1$1:int
// TestKt:13: $this$foo:java.lang.String, count:int, x:boolean, $i$f$block:int, $this$block:long, $i$a$-block-TestKt$foo$1:int
// TestKt:14: $this$foo:java.lang.String, count:int, x:boolean
// TestKt:20: