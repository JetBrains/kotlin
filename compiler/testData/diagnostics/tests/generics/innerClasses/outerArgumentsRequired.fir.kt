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

        val c: <!OTHER_ERROR, OTHER_ERROR!>C<Int>?<!> = null
        val d: <!OTHER_ERROR, OTHER_ERROR!>D?<!> = null

        val innerMost: <!OTHER_ERROR, OTHER_ERROR!>Innermost<String>?<!> = null
    }
}
