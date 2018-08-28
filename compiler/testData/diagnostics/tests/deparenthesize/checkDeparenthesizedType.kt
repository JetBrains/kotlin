// !WITH_NEW_INFERENCE
// !CHECK_TYPE

package m

fun test(i: Int?) {
    if (i != null) {
        foo(<!OI;REDUNDANT_LABEL_WARNING!>l1@<!> <!DEBUG_INFO_SMARTCAST!>i<!>)
        foo((<!DEBUG_INFO_SMARTCAST!>i<!>))
        foo(<!OI;REDUNDANT_LABEL_WARNING!>l2@<!> (<!DEBUG_INFO_SMARTCAST!>i<!>))
        foo((<!OI;REDUNDANT_LABEL_WARNING!>l3@<!> <!DEBUG_INFO_SMARTCAST!>i<!>))
    }

    val a: Int = <!OI;REDUNDANT_LABEL_WARNING!>l4@<!> <!TYPE_MISMATCH!>""<!>
    val b: Int = (<!TYPE_MISMATCH!>""<!>)
    val c: Int = checkSubtype<Int>(<!TYPE_MISMATCH!>""<!>)
    val d: Int = <!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH, TYPE_MISMATCH!>checkSubtype<Long>(<!TYPE_MISMATCH!>""<!>)<!>


    foo(<!OI;REDUNDANT_LABEL_WARNING!>l4@<!> <!TYPE_MISMATCH!>""<!>)
    foo((<!TYPE_MISMATCH!>""<!>))
    foo(checkSubtype<Int>(<!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH, TYPE_MISMATCH!>""<!>))
    foo(<!NI;TYPE_MISMATCH, TYPE_MISMATCH!>checkSubtype<Long>(<!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH, TYPE_MISMATCH!>""<!>)<!>)
    
    use(a, b, c, d)
}

fun foo(i: Int) = i

fun use(vararg a: Any?) = a
