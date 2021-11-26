class Foo(val bar: String?)

fun test(foo: Foo?) {
    foo!!.bar.let {
        // Correct
        foo.bar?.length
        // Unnecessary
        <!SAFE_CALL_WILL_CHANGE_NULLABILITY!>foo<!UNNECESSARY_SAFE_CALL!>?.<!>bar<!>?.length
    }
    <!SAFE_CALL_WILL_CHANGE_NULLABILITY!>foo.bar<!UNNECESSARY_SAFE_CALL!>?.<!>length<!>
}
