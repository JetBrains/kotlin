enum class E {
    I
}

fun box(): String {
    val i = (E::name.getter)(E.I)
    if (i != "I") return "Fail $i"
    return "OK"
}