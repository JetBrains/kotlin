fun box(): String {
    for (x in DoubleArray(5)) {
        if (x != 0.toDouble()) return "Fail $x"
    }
    return "OK"
}
