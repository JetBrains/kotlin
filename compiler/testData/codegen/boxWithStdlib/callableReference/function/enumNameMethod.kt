enum class E {
    I
}

fun box(): String {
    val i = E.I.(E::name)()
    if (i != "I") return "Fail $i"
    return "OK"
}
