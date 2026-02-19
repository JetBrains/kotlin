fun box(): String {
    val x: Any = "OK"

    return if ((x as String) == "a") {
        "FAIL1"
    } else if ((x as String) == "OK") {
        "OK"
    } else {
        "FAIL2"
    }
}
