data class D(private val x: Long, private val y: Char)

fun box(): String {
    val d1 = D(42L, 'a')
    val d2 = D(42L, 'a')
    if (d1 != d2) return "Fail equals"
    if (d1.hashCode() != d2.hashCode()) return "Fail hashCode"
    if (d1.toString() != d2.toString()) return "Fail toString"
    return "OK"
}
