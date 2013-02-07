open data class A(open val x: String)

class B : A("Fail") {
    override val x: String = "OK"
}

fun foo(a: A) = a

fun box(): String {
    return if ("${foo(B())}" == "A(x=OK)") "OK" else "fail"
}
