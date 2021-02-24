class C {
    fun xf1(){}
    fun xf1(s: String){}
}

fun foo(p: (String) -> Unit){}

fun bar(c: C) {
    foo(c::xf1)
}
