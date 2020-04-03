// !DUMP_CFG
fun test_1(b1: Boolean, b2: Boolean) {
    if (b1 || b2) {
        1
    }
}

fun test_2(b1: Boolean, b2: Boolean) {
    if (b1 && b2) {
        1
    }
}

fun test_3(b1: Boolean, b2: Boolean, b3: Boolean) {
    if (b1 && b2 || b3) {
        1
    }
}

fun test_4(b1: Boolean, b2: Boolean, b3: Boolean) {
    if (b1 || b2 && b3) {
        1
    }
}
