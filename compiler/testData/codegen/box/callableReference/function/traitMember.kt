// IGNORE_BACKEND: JVM_IR
interface A {
    fun foo(): String
}

class B : A {
    override fun foo() = "OK"
}

fun box() = (A::foo)(B())
