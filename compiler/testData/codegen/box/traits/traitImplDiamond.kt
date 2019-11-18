// IGNORE_BACKEND_FIR: JVM_IR
interface A {
    fun foo() = "Fail"
}

interface B : A

interface C : A {
    override fun foo() = "OK"
}

interface D : B, C

class Impl : D

fun box(): String = Impl().foo()
