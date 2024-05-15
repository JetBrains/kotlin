// FIR_IDENTICAL
// ISSUE: KT-67503

@Target(AnnotationTarget.TYPE)
annotation class Ann

class Inv<T>

fun <T> foo(x: Inv<@Ann <!SYNTAX!>*<!>>) {}
fun <T> foo2(x: Inv<@Ann<!SYNTAX!><!> >) {}