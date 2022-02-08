// !DIAGNOSTICS: -UNUSED_VARIABLE

interface Foo<T>
interface Foo2<in <!REPEATED_MODIFIER!>in<!> T>

fun test1(foo: Foo<in out Int>) = foo
fun test2(): Foo<in in Int> = throw Exception()

fun test3() {
    val f: Foo<out out out out Int>

    class Bzz<in <!REPEATED_MODIFIER!>in<!> T>
}

class A {
    fun <<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>out<!> <!REPEATED_MODIFIER!>out<!> T> bar() {
    }
}

fun test4(a: A) {
    a.bar<<!PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT!>out out Int<!>>()
}
