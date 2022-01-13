class C(val value: Any)

fun box(): String {
    val c1 = C(-0.0)
    val c2 = C(0.toByte())
    val cmp = (c1.value as Double).compareTo(c2.value as Byte)
    if (cmp != -1) return "Failed: cmp=$cmp"
    return "OK"
}