// !LANGUAGE: -NewCapturedReceiverFieldNamingConvention
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
// TestKt:4: $receiver:java.lang.String, count:int
// TestKt:6: $receiver:java.lang.String, count:int, x:boolean
// TestKt:17: block:TestKt$foo$1
// TestKt$foo$1:7: $receiver:long
// TestKt$foo$1:8: $receiver:long, y:boolean
// TestKt:17: block:TestKt$foo$1$1
// TestKt$foo$1$1:9: $receiver:long
// TestKt$foo$1$1:10: $receiver:long, z:boolean
// TestKt:17: block:TestKt$foo$1$1$1
// TestKt$foo$1$1:13: $receiver:long
// TestKt$foo$1$1.invoke(java.lang.Object)+11:
// TestKt$foo$1$1.invoke(java.lang.Object)+14:
// TestKt:17: block:TestKt$foo$1$1
// TestKt$foo$1:14: $receiver:long
// TestKt$foo$1.invoke(java.lang.Object)+11:
// TestKt$foo$1.invoke(java.lang.Object)+14:
// TestKt:17: block:TestKt$foo$1
// TestKt:15: $receiver:java.lang.String, count:int, x:boolean
// TestKt:21: