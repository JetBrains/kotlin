open data class A(open val x: String)

class B : A("Fail") {
    override val x: String = "OK"
}

fun foo(a: A) = a.component1()

fun box(): String {
    return foo(B())
}
