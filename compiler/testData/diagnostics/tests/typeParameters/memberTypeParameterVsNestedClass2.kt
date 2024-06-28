// ISSUE: KT-63377
// FIR_DUMP

class Outer {
    class T

    inline fun <reified T> foo() {
        T::class
        val x: T? = null
        val y: T? = <!TYPE_MISMATCH!>T()<!>
    }

    fun <T> bar() {
        <!TYPE_PARAMETER_AS_REIFIED!>T::class<!>
        val x: T? = null
        val y: T? = <!TYPE_MISMATCH!>T()<!>
    }
}

class Owner<T> {
    class T

    fun baz() {
        <!TYPE_PARAMETER_AS_REIFIED!>T::class<!>
        val x: T? = null
        val y: T? = <!TYPE_MISMATCH!>T()<!>
    }
}
