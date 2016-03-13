class A

fun A.foo() = "OK"

fun box(): String {
    val x = A::foo
    return x(A())
}
