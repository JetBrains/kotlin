fun box(): String {
    Unit

    val a = Unit
    val b = Unit
    if (a != b) return "Fail a != b"

    if (Unit != Unit) return "Fail Unit != Unit"

    if (a.VALUE != Unit.VALUE) return "Fail a.VALUE != Unit.VALUE"

    return "OK"
}
