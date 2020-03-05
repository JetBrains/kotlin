// !DUMP_CFG
fun test_1(x: Int?) {
    val y = if (x == null) {
        throw Exception()
    } else {
        x
    }
    y.inc()
    x.inc()
}

fun test_2(x: Int?) {
    val y = if (x == null) {
        x
    } else {
        x
    }
    y.<!INAPPLICABLE_CANDIDATE!>inc<!>()
}

fun test_3(x: Int?) {
    while (true) {
        x as Int
        break
    }
    x.inc()
}

fun test_4(x: Int?) {
    do {
        x as Int
        break
    } while (true)
    x.inc()
}

fun test_5(b: Boolean) {
    while (b) {
        if (b) {
            continue
        }
    }
}

inline fun run(block: () -> Unit) {
    block()
}

fun test_6() {
    run {
        return@run
    }
}