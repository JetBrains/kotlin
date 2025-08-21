// LANGUAGE: +DataFlowBasedExhaustiveness
// IGNORE_BACKEND_K1: ANY

fun foo(b: Boolean): Int {
    if (b == false) return 1
    return when (b) {
        true -> 2
    }
}

fun qux(b: Boolean?): Int {
    if ((b == true) == false) return 1
    return when (b) {
        true -> 2
    }
}

fun quux(b: Boolean?): Int {
    if ((b == true) == true) return 1
    return when (b) {
        null -> 2
        false -> 3
    }
}

fun foo(b: Boolean?): Int {
    if (b == null) return 1
    return when (b) {
        true -> 2
        false -> 3
    }
}

fun box(): String {
    if (foo(false) != 1) return "Fail1"
    if (foo(true) != 2) return "Fail2"

    if (qux(null) != 1) return "Fail3"
    if (qux(false) != 1) return "Fail4"
    if (qux(true) != 2) return "Fail5"

    if (quux(true) != 1) return "Fail6"
    if (quux(null) != 2) return "Fail7"
    if (quux(false) != 3) return "Fail8"

    return "OK"
}