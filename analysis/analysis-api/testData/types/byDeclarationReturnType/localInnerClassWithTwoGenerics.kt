class A<AA1, AA2> {
    inner class B<BB1, BB2> {
        fun <FOO1, FOO2> foo() {
            class C<CC1, CC2> {
                inner class D<DD1, DD2> {
                    fun f<caret>oo(): D<FOO1, String>? = null
                }
            }
        }
    }
}
