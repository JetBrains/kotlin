// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR

data class A(var x: Int) : Cloneable {
    public override fun clone(): A = A(x)
}

fun box(): String {
    val a = A(42)
    val b = a.clone()
    if (b != a) return "Fail equals"
    if (b === a) return "Fail identity"
    return "OK"
}
