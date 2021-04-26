class Foo(val bar: String?)

fun test(foo: Foo?) {
    foo!!.bar.let {
        // Correct
        foo.bar?.length
        // Unnecessary
        foo<!UNNECESSARY_SAFE_CALL!>?.<!>bar?.length
    }
    foo.bar<!UNNECESSARY_SAFE_CALL!>?.<!>length
}
