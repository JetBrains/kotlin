// IGNORE_BACKEND_FIR: JVM_IR
interface A {
    val result: String
}

class Base(override val result: String) : A

open class Derived : A by Base("OK")

class Z : Derived()

fun box() = Z().result
