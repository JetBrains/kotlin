// !WITH_NEW_INFERENCE
// !CHECK_TYPE

package m

fun test(i: Int?) {
    if (i != null) {
        foo(l1@ <!DEBUG_INFO_SMARTCAST!>i<!>)
        foo((<!DEBUG_INFO_SMARTCAST!>i<!>))
        foo(l2@ (<!DEBUG_INFO_SMARTCAST!>i<!>))
        foo((l3@ <!DEBUG_INFO_SMARTCAST!>i<!>))
    }

    val a: Int = l4@ <!TYPE_MISMATCH!>""<!>
    val b: Int = (<!TYPE_MISMATCH!>""<!>)
    val c: Int = checkSubtype<Int>(<!TYPE_MISMATCH!>""<!>)
    val d: Int = <!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH, TYPE_MISMATCH!>checkSubtype<Long>(<!TYPE_MISMATCH!>""<!>)<!>


    foo(l4@ <!TYPE_MISMATCH!>""<!>)
    foo((<!TYPE_MISMATCH!>""<!>))
    foo(checkSubtype<Int>(<!TYPE_MISMATCH!>""<!>))
    foo(<!TYPE_MISMATCH!>checkSubtype<Long>(<!TYPE_MISMATCH!>""<!>)<!>)
    
    use(a, b, c, d)
}

fun foo(i: Int) = i

fun use(vararg a: Any?) = a
