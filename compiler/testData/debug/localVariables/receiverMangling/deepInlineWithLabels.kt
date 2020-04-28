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
// TestKt:3: LV:$this$foo:java.lang.String, LV:count:int
// TestKt:5: LV:$this$foo:java.lang.String, LV:count:int, LV:x:boolean
// TestKt:16: LV:$this$foo:java.lang.String, LV:count:int, LV:x:boolean, LV:$i$f$block:int
// TestKt:6: LV:$this$foo:java.lang.String, LV:count:int, LV:x:boolean, LV:$i$f$block:int, LV:$this$b1:long, LV:$i$a$-block-TestKt$foo$1:int
// TestKt:7: LV:$this$foo:java.lang.String, LV:count:int, LV:x:boolean, LV:$i$f$block:int, LV:$this$b1:long, LV:$i$a$-block-TestKt$foo$1:int, LV:y:boolean
// TestKt:16: LV:$this$foo:java.lang.String, LV:count:int, LV:x:boolean, LV:$this$b1:long, LV:$i$a$-block-TestKt$foo$1:int, LV:y:boolean, LV:$i$f$block:int
// TestKt:8: LV:$this$foo:java.lang.String, LV:count:int, LV:x:boolean, LV:$this$b1:long, LV:$i$a$-block-TestKt$foo$1:int, LV:y:boolean, LV:$i$f$block:int, LV:$this$b2:long, LV:$i$a$-block-TestKt$foo$1$1:int
// TestKt:9: LV:$this$foo:java.lang.String, LV:count:int, LV:x:boolean, LV:$this$b1:long, LV:$i$a$-block-TestKt$foo$1:int, LV:y:boolean, LV:$i$f$block:int, LV:$this$b2:long, LV:$i$a$-block-TestKt$foo$1$1:int, LV:z:boolean
// TestKt:16: LV:$this$foo:java.lang.String, LV:count:int, LV:x:boolean, LV:$this$b1:long, LV:$i$a$-block-TestKt$foo$1:int, LV:y:boolean, LV:$this$b2:long, LV:$i$a$-block-TestKt$foo$1$1:int, LV:z:boolean, LV:$i$f$block:int
// TestKt:10: LV:$this$foo:java.lang.String, LV:count:int, LV:x:boolean, LV:$this$b1:long, LV:$i$a$-block-TestKt$foo$1:int, LV:y:boolean, LV:$this$b2:long, LV:$i$a$-block-TestKt$foo$1$1:int, LV:z:boolean, LV:$i$f$block:int, LV:$this$b3:long, LV:$i$a$-block-TestKt$foo$1$1$1:int
// TestKt:11: LV:$this$foo:java.lang.String, LV:count:int, LV:x:boolean, LV:$this$b1:long, LV:$i$a$-block-TestKt$foo$1:int, LV:y:boolean, LV:$this$b2:long, LV:$i$a$-block-TestKt$foo$1$1:int, LV:z:boolean, LV:$i$f$block:int, LV:$this$b3:long, LV:$i$a$-block-TestKt$foo$1$1$1:int
// TestKt:12: LV:$this$foo:java.lang.String, LV:count:int, LV:x:boolean, LV:$this$b1:long, LV:$i$a$-block-TestKt$foo$1:int, LV:y:boolean, LV:$i$f$block:int, LV:$this$b2:long, LV:$i$a$-block-TestKt$foo$1$1:int
// TestKt:13: LV:$this$foo:java.lang.String, LV:count:int, LV:x:boolean, LV:$i$f$block:int, LV:$this$b1:long, LV:$i$a$-block-TestKt$foo$1:int
// TestKt:14: LV:$this$foo:java.lang.String, LV:count:int, LV:x:boolean
// TestKt:20: