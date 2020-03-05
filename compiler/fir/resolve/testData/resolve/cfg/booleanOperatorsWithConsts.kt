// !DUMP_CFG
fun test_1(b: Boolean) {
    if (b || false) {
        1
    }
}

fun test_2(b: Boolean) {
    if (false || b) {
        1
    }
}

fun test_3(b: Boolean) {
    if (b || true) {
        1
    }
}

fun test_4(b: Boolean) {
    if (true || b) {
        1
    }
}

fun test_5(b: Boolean) {
    if (b && false) {
        1
    }
}

fun test_6(b: Boolean) {
    if (false && b) {
        1
    }
}

fun test_7(b: Boolean) {
    if (b && true) {
        1
    }
}

fun test_8(b: Boolean) {
    if (true && b) {
        1
    }
}