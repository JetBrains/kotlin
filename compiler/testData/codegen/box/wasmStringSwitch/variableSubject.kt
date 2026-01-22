object C {
    var c = 0
}

fun foo(): String {
    C.c++
    return "b"
}

fun box(): String {
    C.c = 0

    val v = foo()

    return if (v == "a") {
        "FAIL1"
    } else if (v == "b") {
        if (C.c != 1) {
            return "FAIL2"
        }
        "OK"
    } else {
        "FAIL3"
    }
}
