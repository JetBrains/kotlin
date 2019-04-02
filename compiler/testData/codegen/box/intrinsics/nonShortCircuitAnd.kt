var s = ""

fun o(): Boolean {
    s += "O"
    return false
}

fun k(): Boolean {
    s += "K"
    return true
}

fun box(): String {
    val b = o() and k()
    if (b)
        return "fail: b should be false"
    return s
}