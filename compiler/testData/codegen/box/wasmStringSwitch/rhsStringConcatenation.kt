fun box(): String {
    val a = "O"
    val b = a + "K"

    return if (b == "FAIL") {
        "FAIL1"
    } else if (b == "OK") {
        "OK"
    } else {
        "FAIL2"
    }
}
