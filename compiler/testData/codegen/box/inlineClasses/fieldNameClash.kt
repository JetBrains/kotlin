inline class Z(val s: String) {
    val Int.s: Int get() = 42
}

fun box(): String {
    if (Z("a").toString() == "Z(s=a)")
        return "OK"
    return "Fail"
}
