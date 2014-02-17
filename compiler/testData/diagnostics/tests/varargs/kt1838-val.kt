class A(vararg val t : Int) {
    {
        val <!UNUSED_VARIABLE!>t1<!> : IntArray = t
    }
}
