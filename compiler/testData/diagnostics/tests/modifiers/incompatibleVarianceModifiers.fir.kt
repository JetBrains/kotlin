// !DIAGNOSTICS: -UNUSED_VARIABLE

interface Foo<T>
interface Foo1<<!INCOMPATIBLE_MODIFIERS!>in<!> <!INCOMPATIBLE_MODIFIERS!>out<!> T>
interface Foo2<in <!REPEATED_MODIFIER!>in<!> T>

fun test1(foo: Foo<in out Int>) = foo
fun test2(): Foo<in in Int> = throw Exception()

fun test3() {
    val f: Foo<out out out out Int>

    class Bzz<in <!REPEATED_MODIFIER!>in<!> T>
}

class A {
    fun <out <!REPEATED_MODIFIER!>out<!> T> bar() {
    }
}

fun test4(a: A) {
    a.bar<out out Int>()
}
