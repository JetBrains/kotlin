// IGNORE_BACKEND_FIR: JVM_IR
abstract class A {
    abstract fun foo(): String
}

class B : A() {
    override fun foo() = "OK"
}

fun box(): String = (A::foo)(B())
