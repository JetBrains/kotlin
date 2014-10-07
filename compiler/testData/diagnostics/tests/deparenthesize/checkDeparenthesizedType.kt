package m

fun test(i: Int?) {
    if (i != null) {
        foo(@l1 <!DEBUG_INFO_SMARTCAST!>i<!>)
        foo((<!DEBUG_INFO_SMARTCAST!>i<!>))
        foo(@l2 (<!DEBUG_INFO_SMARTCAST!>i<!>))
        foo((@l3 <!DEBUG_INFO_SMARTCAST!>i<!>))
    }

    val a: Int = @l4 <!TYPE_MISMATCH!>""<!>
    val b: Int = (<!TYPE_MISMATCH!>""<!>)
    val c: Int = <!TYPE_MISMATCH!>""<!>: Int
    val d: Int = <!TYPE_MISMATCH!><!TYPE_MISMATCH!>""<!>: Long<!>


    foo(@l4 <!TYPE_MISMATCH!>""<!>)
    foo((<!TYPE_MISMATCH!>""<!>))
    foo(<!TYPE_MISMATCH!>""<!>: Int)
    foo(<!TYPE_MISMATCH!><!TYPE_MISMATCH!>""<!>: Long<!>)
    
    use(a, b, c, d)
}

fun foo(i: Int) = i

fun use(vararg a: Any?) = a
