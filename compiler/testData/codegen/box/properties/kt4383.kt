class D {
    fun get(a: Any, p: PropertyMetadata) { }
}

object P {
    val u = Unit
    val v by D()
    var w = Unit
}

fun box(): String {
    if (P.u != P.v) return "Fail uv"
    P.w = Unit
    if (P.w != P.u) return "Fail w"
    return "OK"
}
