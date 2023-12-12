// FIR_IDENTICAL
// KT-64271
// IGNORE_BACKEND_K2: JVM_IR

open class Base {
    override fun equals(other: Any?): Boolean {
        return this === other
    }
}

interface I {

}

class Child1 : Base(), I
class Child2 : I, Base()