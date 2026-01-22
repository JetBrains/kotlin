fun box(): String {
    val a = "a"
    val b = "b"

    if (a == "b") {
        return "FAIL1"
    } else if (b == "b") {
        return "OK"
    } else if (a == "c") {
        return "FAIL2"
    } else if (b == "c") {
        return "FAIL3"
    } else {
        return "FAIL4"
    }
}