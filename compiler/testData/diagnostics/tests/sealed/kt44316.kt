// FIR_IDENTICAL
// KT-44316

sealed class Base
class Derived : Base()

class Test<out V>(val x: Base) {
    private val y = when (x) {
        is Derived -> null
    }
}
