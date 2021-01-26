import A.B.D
import A.B.C
import A.B.D.Innermost

class A<T> {
    inner class B<F> {
        inner class C<E>
        inner class D {
            inner class Innermost<X>
        }
    }

    class Nested {
        val x: B<String>? = null
        val y: B<String>.C<String>? = null
        val z: B<String>.D? = null

        val c: <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>C<Int>?<!> = null
        val d: <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>D?<!> = null

        val innerMost: <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Innermost<String>?<!> = null
    }
}
