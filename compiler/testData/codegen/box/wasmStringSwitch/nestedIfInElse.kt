fun box(): String {
    val a = "a"
    val b = "b"

    if (a == "b") {
        return "FAIL1"
    } else {
        if (b == "b") {
            return "OK"
        } else {
            return "FAIL2"
        }
    }
}
