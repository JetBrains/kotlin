fun run(block: () -> Unit) {
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