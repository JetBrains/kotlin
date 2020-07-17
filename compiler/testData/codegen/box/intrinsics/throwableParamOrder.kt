var res = ""

fun getM(): String {
    res += "M"
    return "OK"
}

fun getT(): Throwable {
    res += "T"
    return Throwable("test", null)
}

fun box(): String {
    val z = Throwable(cause = getT(), message = getM())
    if (res != "TM") return "Wrong argument calculation order: $res"
    return z.message!!
}