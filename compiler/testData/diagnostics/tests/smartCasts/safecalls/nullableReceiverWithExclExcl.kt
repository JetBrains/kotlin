class Foo(val bar: String?)

fun test(foo: Foo?) {
    foo!!.bar.let {
        // Correct
        <!DEBUG_INFO_SMARTCAST!>foo<!>.bar?.length
        // Unnecessary
        foo<!UNNECESSARY_SAFE_CALL!>?.<!>bar?.length
    }
    <!DEBUG_INFO_SMARTCAST!>foo<!>.bar?.length
}