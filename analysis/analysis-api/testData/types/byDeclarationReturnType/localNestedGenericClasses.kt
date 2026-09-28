fun <F> foo() {
    class B<BB> {
        class C<CC> {
            fun g<caret>g(): C<Int>? = null
        }
    }
}
