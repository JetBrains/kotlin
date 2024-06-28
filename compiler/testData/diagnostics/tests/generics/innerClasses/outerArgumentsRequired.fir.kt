// WITH_STDLIB
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
        val x: <!OUTER_CLASS_ARGUMENTS_REQUIRED("class 'A'")!>B<String>?<!> = null
        val y: <!OUTER_CLASS_ARGUMENTS_REQUIRED("class 'A'")!>B<String>.C<String>?<!> = null
        val z: <!OUTER_CLASS_ARGUMENTS_REQUIRED("class 'A'")!>B<String>.D?<!> = null

        val c: <!OUTER_CLASS_ARGUMENTS_REQUIRED("class 'B'")!>C<Int>?<!> = null
        val d: <!OUTER_CLASS_ARGUMENTS_REQUIRED("class 'B'")!>D?<!> = null

        val innerMost: <!OUTER_CLASS_ARGUMENTS_REQUIRED("class 'B'")!>Innermost<String>?<!> = null

        fun foo() {
            object {
                val something = listOf<<!OUTER_CLASS_ARGUMENTS_REQUIRED!>B<String><!>>()
            }
        }
    }

    fun foo() {
        object {
            val something = listOf<B<String>>() // False positive in K1 KT-63732
        }
    }
}

fun <T> bar() {
    data class Example(val foo: Int)
    object {
        val something = listOf<Example>()
    }
}
