// !LANGUAGE: +NewCapturedReceiverFieldNamingConvention
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

fun block(block: Long.() -> Unit) = 5L.block()

fun box() {
    "OK".foo(42)
}

// IGNORE_BACKEND: JVM_IR

// LOCAL VARIABLES
// TestKt:20:
// TestKt:4: $this$foo:java.lang.String, count:int
// TestKt:6: $this$foo:java.lang.String, count:int, x:boolean
// TestKt:17: block:TestKt$foo$1
// TestKt$foo$1:7: $this$block:long
// TestKt$foo$1:8: $this$block:long, y:boolean
// TestKt:17: block:TestKt$foo$1$1
// TestKt$foo$1$1:9: $this$block:long
// TestKt$foo$1$1:10: $this$block:long, z:boolean
// TestKt:17: block:TestKt$foo$1$1$1
// TestKt$foo$1$1:13: $this$block:long
// TestKt$foo$1$1.invoke(java.lang.Object)+11:
// TestKt$foo$1$1.invoke(java.lang.Object)+14:
// TestKt:17: block:TestKt$foo$1$1
// TestKt$foo$1:14: $this$block:long
// TestKt$foo$1.invoke(java.lang.Object)+11:
// TestKt$foo$1.invoke(java.lang.Object)+14:
// TestKt:17: block:TestKt$foo$1
// TestKt:15: $this$foo:java.lang.String, count:int, x:boolean
// TestKt:21: