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
// TestKt:6: $this$foo:java.lang.String, count:int, x:boolean, $i$f$block:int, $this$b1:long, $i$a$-block-TestKt$foo$1:int
// TestKt:7: $this$foo:java.lang.String, count:int, x:boolean, $i$f$block:int, $this$b1:long, $i$a$-block-TestKt$foo$1:int, y:boolean
// TestKt:16: $this$foo:java.lang.String, count:int, x:boolean, $this$b1:long, $i$a$-block-TestKt$foo$1:int, y:boolean, $i$f$block:int
// TestKt:8: $this$foo:java.lang.String, count:int, x:boolean, $this$b1:long, $i$a$-block-TestKt$foo$1:int, y:boolean, $i$f$block:int, $this$b2:long, $i$a$-block-TestKt$foo$1$1:int
// TestKt:9: $this$foo:java.lang.String, count:int, x:boolean, $this$b1:long, $i$a$-block-TestKt$foo$1:int, y:boolean, $i$f$block:int, $this$b2:long, $i$a$-block-TestKt$foo$1$1:int, z:boolean
// TestKt:16: $this$foo:java.lang.String, count:int, x:boolean, $this$b1:long, $i$a$-block-TestKt$foo$1:int, y:boolean, $this$b2:long, $i$a$-block-TestKt$foo$1$1:int, z:boolean, $i$f$block:int
// TestKt:10: $this$foo:java.lang.String, count:int, x:boolean, $this$b1:long, $i$a$-block-TestKt$foo$1:int, y:boolean, $this$b2:long, $i$a$-block-TestKt$foo$1$1:int, z:boolean, $i$f$block:int, $this$b3:long, $i$a$-block-TestKt$foo$1$1$1:int
// TestKt:11: $this$foo:java.lang.String, count:int, x:boolean, $this$b1:long, $i$a$-block-TestKt$foo$1:int, y:boolean, $this$b2:long, $i$a$-block-TestKt$foo$1$1:int, z:boolean, $i$f$block:int, $this$b3:long, $i$a$-block-TestKt$foo$1$1$1:int
// TestKt:12: $this$foo:java.lang.String, count:int, x:boolean, $this$b1:long, $i$a$-block-TestKt$foo$1:int, y:boolean, $i$f$block:int, $this$b2:long, $i$a$-block-TestKt$foo$1$1:int
// TestKt:13: $this$foo:java.lang.String, count:int, x:boolean, $i$f$block:int, $this$b1:long, $i$a$-block-TestKt$foo$1:int
// TestKt:14: $this$foo:java.lang.String, count:int, x:boolean
// TestKt:20: