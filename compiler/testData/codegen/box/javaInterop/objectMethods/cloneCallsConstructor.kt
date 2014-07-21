data class A(var x: Int) : Cloneable {
    public override fun clone(): A = A(x)
}

fun box(): String {
    val a = A(42)
    val b = a.clone()
    if (b != a) return "Fail equals"
    if (b identityEquals a) return "Fail identity"
    return "OK"
}
