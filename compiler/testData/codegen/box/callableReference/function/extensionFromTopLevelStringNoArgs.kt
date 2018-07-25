// IGNORE_BACKEND: JVM_IR
class A

fun A.foo() = "OK"

fun box(): String {
    val x = A::foo
    return x(A())
}
