// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +SealedInlineClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class Marker(val i: Int)

interface I<T> {
    fun foo(i: Marker) : T
}

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class SIC

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC(val a: Any): SIC()

class C : I<SIC> {
    override fun foo(i: Marker): SIC = IC("OK")
}

fun box(): String {
    val i: I<SIC> = C()
    val foo: SIC = i.foo(Marker(0))
    if ((foo as IC).a != "OK") return "FAIL 1"
    val foo1: SIC = C().foo(Marker(0))
    if ((foo1 as IC).a != "OK") return "FAIL 2"
    return "OK"
}
