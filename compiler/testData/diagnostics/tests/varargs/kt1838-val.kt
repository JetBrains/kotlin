class A(vararg val t : Int) {
    init {
        val <!UNUSED_VARIABLE!>t1<!> : IntArray = t
    }
}
