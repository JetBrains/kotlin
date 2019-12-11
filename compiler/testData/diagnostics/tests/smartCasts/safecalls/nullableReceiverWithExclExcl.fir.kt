class Foo(val bar: String?)

fun test(foo: Foo?) {
    foo!!.bar.let {
        // Correct
        foo.bar?.length
        // Unnecessary
        foo?.bar?.length
    }
    foo.bar?.length
}