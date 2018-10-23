// LOCAL_VARIABLE_TABLE
// LANGUAGE_VERSION: 1.3

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