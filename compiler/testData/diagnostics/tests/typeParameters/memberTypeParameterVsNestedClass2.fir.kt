// ISSUE: KT-63377
// FIR_DUMP

class Outer {
    class T

    inline fun <reified T> foo() {
        T::class
        val x: T? = null
        val y: T? = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>T()<!>
    }

    fun <T> bar() {
        T::class
        val x: T? = null
        val y: T? = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>T()<!>
    }
}

class Owner<T> {
    class T

    fun baz() {
        T::class
        val x: T? = null
        val y: T? = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>T()<!>
    }
}
