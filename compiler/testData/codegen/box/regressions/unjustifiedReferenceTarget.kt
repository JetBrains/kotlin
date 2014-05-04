class A

fun foo(plusAssign: A.(A) -> Unit) {
    A() += A()
}

fun box(): String {
    foo { }
    return "OK"
}
