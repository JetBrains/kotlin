fun box(): String {
    val a = "a"
    val b = "b"

    if (a == "c") {
        return "FAIL1"
    } else if (a == "d") {
        return "FAIL2"
    } else if (b == "b") {
        return "OK"
    } else {
        return "FAIL3"
    }
}
