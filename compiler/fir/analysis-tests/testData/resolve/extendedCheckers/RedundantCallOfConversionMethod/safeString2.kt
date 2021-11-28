// WITH_STDLIB
data class Foo(val name: String)

fun test(foo: Foo?) {
    val <!UNUSED_VARIABLE!>s<!>: String? = foo?.name?.<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toString()<!>
}
