// FIR_IDENTICAL
// KT-64271

open class Base {
    override fun equals(other: Any?): Boolean {
        return this === other
    }
}

interface I {

}

class Child1 : Base(), I
class Child2 : I, Base()