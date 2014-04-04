open data class A(val value: String)

trait B {
    fun component1(): Any
}

class C(value: String) : A(value), B

fun box(): String {
    val c = C("OK")
    if ((c : B).component1() != "OK") return "Fail 1"
    if ((c : A).component1() != "OK") return "Fail 2"
    return c.component1()
}
