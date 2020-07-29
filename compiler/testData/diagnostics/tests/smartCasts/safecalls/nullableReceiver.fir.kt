// See KT-10056
class Foo(val bar: String)

fun test(foo: Foo?) {
    foo?.bar.let {
        // Error, foo?.bar is nullable
        it.<!INAPPLICABLE_CANDIDATE!>length<!>
        // Error, foo is nullable
        foo.<!INAPPLICABLE_CANDIDATE!>bar<!>.length
        // Correct
        foo?.bar?.length
    }
}
