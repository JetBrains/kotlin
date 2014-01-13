class D {
    fun get(a: Any, p: PropertyMetadata) { }
}

object P {
    val u = Unit.VALUE
    val v by D()
    var w = Unit.VALUE
}

fun box(): String {
    if (P.u != P.v) return "Fail uv"
    P.w = Unit.VALUE
    if (P.w != P.u) return "Fail w"
    return "OK"
}
