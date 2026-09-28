fun <T> foo() {
    class A<AA> {
        inner class B<BB> {
            inner class C<CC> {
                fun bar() {
                    val r<caret>es = C<Int>()
                }
            }
        }
    }
}
