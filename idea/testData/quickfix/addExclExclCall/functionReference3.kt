// "Add non-null asserted (!!) call" "true"
class Foo {
    fun f() = 1
}

fun Foo?.test() {
    val f = ::f<caret>
}
// TODO: Enable when FIR reports UNSAFE_CALL for function reference on nullable (currently UNRESOLVED_REFERENCE)
/* IGNORE_FIR */
