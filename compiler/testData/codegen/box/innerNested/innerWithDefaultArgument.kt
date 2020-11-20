
// KT-40771


class Outer(val o: String, val oo: String = o) {
    inner class Inner(val k: String, val kk: String = k)
}

fun box(): String {
    val o = Outer("O")
    val i = o.Inner("K")

    return o.oo + i.kk
}