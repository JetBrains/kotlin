// LOCAL_VARIABLE_TABLE

fun String.foo(count: Int) {
    val x: Boolean = false

    block {
        this@foo + this@block.toString() + x.toString() + count.toString()
    }
}

inline fun block(block: Long.() -> Unit) = 5L.block()