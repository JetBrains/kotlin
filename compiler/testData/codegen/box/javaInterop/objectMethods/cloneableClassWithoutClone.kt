data class A(val s: String) : Cloneable {
    fun externalClone(): A = clone() as A
}

fun box(): String {
    val a = A("OK")
    val b = a.externalClone()
    if (a != b) return "Fail equals"
    if (a identityEquals b) return "Fail identity"
    return b.s
}
