fun box(): String {
    val s: String? = "b"

    return if (s == "a") {
        "FAIL1"
    } else if (s == "b") {
        "OK"
    } else if (s == null) {
        "FAIL2"
    } else {
        "FAIL3"
    }
}
