// !LANGUAGE: -NewCapturedReceiverFieldNamingConvention
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
// TestKt:14:
// TestKt:4: LV:$receiver:java.lang.String, LV:count:int
// TestKt:6: LV:$receiver:java.lang.String, LV:count:int, LV:x:boolean
// TestKt:11: LV:block:TestKt$foo$1
// TestKt:9: LV:$receiver:java.lang.String, LV:count:int, LV:x:boolean
// TestKt:15: