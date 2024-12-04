class C(val value: Any)

fun box(): String {
    val c1 = C(1.1)
    val c2 = C(1)
    val cmp = (c1.value as Double).compareTo(c2.value as Int)
    if (cmp != 1) return "Failed: cmp=$cmp"
    return "OK"
}