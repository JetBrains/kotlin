enum class E {
    I
}

fun box(): String {
    val i = (E::name)(E.I)
    if (i != "I") return "Fail $i"
    return "OK"
}
