// "Add non-null asserted (!!) call" "true"
class Foo {
    val bar = Bar()
}

class Bar {
    fun f() = 1
}

fun test(foo: Foo?) {
    val f = foo?.bar::f<caret>
}
// TODO: Enable when FIR reports UNSAFE_CALL for function reference on nullable (currently UNRESOLVED_REFERENCE)
/* IGNORE_FIR */
