// FIR_IDENTICAL

fun String.foo(count: Int) {
    val x = false

    block {
        val y = false
        block {
            val z = true
            block {
                this@foo + this<!LABEL_NAME_CLASH!>@block<!>.toString() + x + y + z + count
            }
        }
    }
}

fun block(block: Long.() -> Unit) = 5L.block()
