fun box(): String {
    val s = "b"

    return if ("a" == s) {
        "FAIL1"
    } else if ("b" == s) {
        "OK"
    } else if ("c" == s) {
        "FAIL2"
    } else {
        "FAIL3"
    }
}
