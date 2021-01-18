// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
import Outer.Inner


class Outer<E> {
    inner class Inner

    fun foo() {
        class E
        val x: Inner = Inner()
    }

    class Nested {
        fun bar(x: Inner) {}
    }
}

class E

fun bar(x: <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Inner<!>) {}
