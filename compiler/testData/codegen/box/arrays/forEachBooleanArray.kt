fun box(): String {
    for (x in BooleanArray(5)) {
        if (x != false) return "Fail $x"
    }
    return "OK"
}
