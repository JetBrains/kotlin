// JVM_ABI_K1_K2_DIFF: KT-63828
interface A {
    val result: String
}

class Base(override val result: String) : A

open class Derived : A by Base("OK")

class Z : Derived()

fun box() = Z().result
