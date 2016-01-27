interface Foo
fun foo(): Foo? = null

val foo: Foo = run {
    run {
        val x = foo()
        if (x == null) throw Exception()
        <!DEBUG_INFO_SMARTCAST!>x<!>
    }
}
