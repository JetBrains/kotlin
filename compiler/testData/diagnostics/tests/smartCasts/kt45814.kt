// SKIP_TXT
// ISSUE: KT-45814

class Foo(val bar: String?)

fun test_1(foo: Foo?) {
    foo!!.bar.let {
        foo<!UNNECESSARY_SAFE_CALL!>?.<!>bar?.length // Unnecessary
        <!DEBUG_INFO_SMARTCAST!>foo<!>.bar?.length // Correct
        foo<!UNNECESSARY_SAFE_CALL!>?.<!>bar?.length // Unnecessary
    }
    <!DEBUG_INFO_SMARTCAST!>foo<!>.bar?.length
    foo<!UNNECESSARY_SAFE_CALL!>?.<!>bar?.length // Unnecessary
}

fun test_2(foo: Foo?) {
    foo!!.bar.let {
        <!DEBUG_INFO_SMARTCAST!>foo<!>.bar?.length // Correct
        foo<!UNNECESSARY_SAFE_CALL!>?.<!>bar?.length // Unnecessary
        Unit
    }
    <!DEBUG_INFO_SMARTCAST!>foo<!>.bar?.length
}
