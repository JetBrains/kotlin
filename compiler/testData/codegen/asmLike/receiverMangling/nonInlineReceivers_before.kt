// !LANGUAGE: -NewCapturedReceiverFieldNamingConvention
// LOCAL_VARIABLE_TABLE

fun String.foo(count: Int) {
    val x = false

    block {
        this@foo + this@block.toString() + x.toString() + count.toString()
    }
}

fun block(block: Long.() -> Unit) = 5L.block()