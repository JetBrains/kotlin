// !DUMP_CFG
inline fun run(block: () -> Unit) {
    block()
}

fun test_1(x: Any?) {
    if (x is Int) {
        run {
            x.inc()
        }
    }
}

fun test_2(x: Any?) {
    if (x is Int) {
        val lambda = {
            x.inc()
        }
    }
}


inline fun getInt(block: () -> Unit): Int {
    block()
    return 1
}

fun test_3(): Int = getInt { return@test_3 1 }


fun test_4(): Int = getInt(block = { return@test_4 1 })
