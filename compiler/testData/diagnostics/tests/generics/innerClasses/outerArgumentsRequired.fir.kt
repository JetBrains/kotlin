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

        val c: C<Int>? = null
        val d: D? = null

        val innerMost: Innermost<String>? = null
    }
}
