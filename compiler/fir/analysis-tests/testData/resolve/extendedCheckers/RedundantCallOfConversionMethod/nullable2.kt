// IS_APPLICABLE: false
// WITH_STDLIB
data class Foo(val name: String)

fun nullable2(foo: Foo?) {
    val <!UNUSED_VARIABLE!>s<!>: String = foo?.name.toString()
}
