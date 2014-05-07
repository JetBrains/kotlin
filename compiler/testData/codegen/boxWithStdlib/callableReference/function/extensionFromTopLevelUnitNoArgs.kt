class A {
    var result = "Fail"
}

fun A.foo() {
    result = "OK"
}

fun box(): String {
    val a = A()
    val x = A::foo
    a.x()
    return a.result
}
