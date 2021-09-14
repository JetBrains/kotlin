class X<T> {
    inner class Y<T1>
    class Z<T2>

    fun <T3> foo() {
        class U<T4> {
            inner class K<T5>
            class C<T6>
        }
    }
}
