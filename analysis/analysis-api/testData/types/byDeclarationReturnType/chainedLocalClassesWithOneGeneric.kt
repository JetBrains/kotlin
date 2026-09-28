class A {
    fun <FOO1> foo() {
        class B {
            fun <FOO2> foo() {
                class C<CC> 
                val r<caret>ef = C<Int>()
            }
        }
    }
}
