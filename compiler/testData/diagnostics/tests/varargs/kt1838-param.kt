class A(vararg t : Int) {
    init {
        val <!UNUSED_VARIABLE!>t1<!> : IntArray = t
    }
}
