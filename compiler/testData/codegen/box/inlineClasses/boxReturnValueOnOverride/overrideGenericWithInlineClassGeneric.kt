// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Marker(val i: Int)

interface I<T> {
    fun foo(i: Marker) : T
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC<T: Any>(val a: T)

class C : I<IC<String>> {
    override fun foo(i: Marker): IC<String> = IC("OK")
}

fun box(): String {
    val i: I<IC<String>> = C()
    val foo: IC<String> = i.foo(Marker(0))
    if (foo.a != "OK") return "FAIL 1"
    val foo1: IC<String> = C().foo(Marker(0))
    if (foo1.a != "OK") return "FAIL 2"
    return "OK"
}
