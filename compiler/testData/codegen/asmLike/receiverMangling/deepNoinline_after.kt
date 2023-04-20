// !LANGUAGE: +NewCapturedReceiverFieldNamingConvention
// IR_DIFFERENCE
// LOCAL_VARIABLE_TABLE
// LAMBDAS: CLASS

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
