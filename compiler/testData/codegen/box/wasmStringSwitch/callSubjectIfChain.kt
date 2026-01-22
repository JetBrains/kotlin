// WITH_STDLIB

object C {
    var c = 0
}

fun foo(): String {
    C.c++
    return "b"
}

fun box(): String {
    C.c = 0

    if (foo() == "a") {
        return "FAIL1"
    } else if (foo() == "b") {
        if (C.c != 2) {
            return "FAIL2"
        }
        return "OK"
    } else {
        return "FAIL3"
    }
}
