// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
import Outer.Inner


class Outer<E> {
    inner class Inner

    fun foo() {
        class E
        val x: Inner = Inner()
    }

    class Nested {
        fun bar(x: <!OUTER_CLASS_ARGUMENTS_REQUIRED("class 'Outer'")!>Inner<!>) {}
    }
}

class E

fun bar(x: <!OUTER_CLASS_ARGUMENTS_REQUIRED("class 'Outer'")!>Inner<!>) {}
