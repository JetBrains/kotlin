class A<AA> {
    fun <FOO1> foo() {
        class B<BB> {
            fun <FOO2> foo() {
                class C<CC> 
                val r<caret>ef = C<Int>()
            }
        }
    }
}
