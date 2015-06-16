// !DIAGNOSTICS: -UNUSED_VARIABLE

interface Foo<T>
interface Foo1<<!INCOMPATIBLE_MODIFIERS!>in<!> <!INCOMPATIBLE_MODIFIERS!>out<!> T>
interface Foo2<in <!REPEATED_MODIFIER!>in<!> T>

fun test1(foo: Foo<<!INCOMPATIBLE_MODIFIERS!>in<!> <!INCOMPATIBLE_MODIFIERS!>out<!> Int>) = foo
fun test2(): Foo<in <!REPEATED_MODIFIER!>in<!> Int> = throw Exception()

fun test3() {
    val f: Foo<out <!REPEATED_MODIFIER!>out<!> <!REPEATED_MODIFIER!>out<!> <!REPEATED_MODIFIER!>out<!> Int>

    class Bzz<in <!REPEATED_MODIFIER!>in<!> T>
}

class A {
    fun <<!VARIANCE_ON_TYPE_PARAMETER_OF_FUNCTION_OR_PROPERTY!>out<!> <!REPEATED_MODIFIER!>out<!> T> bar() {
    }
}

fun test4(a: A) {
    a.bar<<!PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT!>out<!> <!REPEATED_MODIFIER!>out<!> Int>()
}
