class A<T1, T2> {
    fun foo1() {}

    class B<T3, T4> {
        fun foo2() {}

        fun foo3() {
            foo<caret>
        }
    }

    class C

    companion object {
        fun <T7, T8> B<T7, T8>.foo4() {}
        fun C.foo5() {}
    }
}

// ABSENT: foo1
// EXIST: foo2
// EXIST: foo3
// EXIST: foo4
// ABSENT: foo5
