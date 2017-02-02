class A

fun A.foo(result: String) = result

fun box(): String {
    val x = A::foo
    return x(A(), "OK")
}
