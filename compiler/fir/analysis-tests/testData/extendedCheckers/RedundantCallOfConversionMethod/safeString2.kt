// WITH_RUNTIME
data class Foo(val name: String)

fun test(foo: Foo?) {
    val s: String? = foo?.name?.<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toString()<!>
}