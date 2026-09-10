class A<AA> {
    class B<BB> {
        inner class C<CC> {
            fun foo() {
                class Bar<F>

                val re<caret>s = Bar<Int>()
            }
        }
    }
}
