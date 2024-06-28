// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR

data class A(var x: Int) : Cloneable {
    public override fun clone(): A = super.clone() as A
}

fun box(): String {
    val a = A(42)
    val b = a.clone()
    if (a != b) return "Fail equals"
    if (a === b) return "Fail identity"
    return "OK"
}
