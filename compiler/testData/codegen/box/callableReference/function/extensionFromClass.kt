// IGNORE_BACKEND: JVM_IR
class A {
    fun result() = (A::foo)(this, "OK")
}

fun A.foo(x: String) = x

fun box() = A().result()
