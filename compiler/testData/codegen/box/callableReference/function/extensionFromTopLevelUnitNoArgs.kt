// IGNORE_BACKEND_FIR: JVM_IR
class A {
    var result = "Fail"
}

fun A.foo() {
    result = "OK"
}

fun box(): String {
    val a = A()
    val x = A::foo
    x(a)
    return a.result
}
