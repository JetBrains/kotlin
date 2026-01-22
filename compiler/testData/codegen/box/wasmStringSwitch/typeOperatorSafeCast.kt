fun box(): String {
    val x: Any? = "OK"
    val y: Any? = 42

    if (x as? String == "A") {
        return "FAIL1"
    } else if (x as? String == "OK") {
        if (y as? String == null) {
            return "OK"
        } else {
            return "FAIL2"
        }
    } else {
        return "FAIL3"
    }
}
