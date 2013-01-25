fun box(): String {
    if (3.compareTo(2) != 1) return "Fail #1"
    if (5.toByte().compareTo(10.toLong()) >= 0) return "Fail #2"
    return "OK"
}
