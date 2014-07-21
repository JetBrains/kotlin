data class A(var x: Int) : Cloneable {
    public override fun clone(): A {
        val result = super.clone() as A
        result.x = 239
        return result
    }
}

fun box(): String {
    val a = A(42)
    val b = a.clone()
    if (a == b) return "Fail: $a == $b"
    if (a identityEquals b) return "Fail: $a identityEquals $b"
    if (b.x != 239) return "Fail: b.x = ${b.x}"
    return "OK"
}
