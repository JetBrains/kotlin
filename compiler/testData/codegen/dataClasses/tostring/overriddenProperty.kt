open data class A(open val x: String)

class B : A("OK") {
    override val x: String = "Fail"
}

fun foo(a: A) = a

fun box(): String {
    return if ("${foo(B())}" == "A{x=OK}") "OK" else "fail"
}
