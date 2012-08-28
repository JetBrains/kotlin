open data class A(open val x: String)

class B : A("OK") {
    override val x: String = "Fail"
}

fun foo(a: A) = a.component1()

fun box(): String {
    return foo(B())
}
