class A<AA> {
    class B<BB> {
        inner class C<CC> {
            fun <F> foo() {
                class D<DD> {
                    abstract inner class E<EE>

                    val re<caret>s = object : E<Int>() {

                    }
                }
            }
        }
    }
}
