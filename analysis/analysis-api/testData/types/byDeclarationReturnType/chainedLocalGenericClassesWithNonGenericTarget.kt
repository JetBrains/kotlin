class A<AA> {
    fun <FOO1> foo() {
        class B<BB> {
            fun <FOO2> foo() {
                class C
                val r<caret>ef = C()
            }
        }
    }
}
