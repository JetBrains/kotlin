// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

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
