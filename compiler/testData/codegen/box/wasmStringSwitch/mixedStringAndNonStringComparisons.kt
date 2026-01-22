fun box(): String {
    val s: Any = "b"

    return if (s == "a") {
        "FAIL1"
    } else if (s == "b") {
        "OK"
    } else if (s == 1) {
        "FAIL2"
    } else {
        "FAIL3"
    }
}
