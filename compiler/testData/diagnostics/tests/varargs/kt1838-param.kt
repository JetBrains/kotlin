class A(vararg t : Int) {
    {
        val <!UNUSED_VARIABLE!>t1<!> : IntArray = t
    }
}
