// !DIAGNOSTICS: -UNUSED_VARIABLE
package test

object Wrong
object Right

class a {
    class b<T> {
        class c {
            fun foo() = Wrong
        }
    }
}

fun Int.foo() = Right

class Test {
    val a: List<Int> = null!!

    val <T> List<T>.b: Int get() = 42

    val Int.c: Int get() = 42

    val test1: () -> Right = <!RESERVED_SYNTAX_IN_CALLABLE_REFERENCE_LHS!><!DEBUG_INFO_MISSING_UNRESOLVED!>a<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>b<!><<!DEBUG_INFO_MISSING_UNRESOLVED!>Int<!>>.<!DEBUG_INFO_MISSING_UNRESOLVED!>c<!><!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>foo<!>
    val test1a: () -> Right = a.b.c::foo

    val test2: () -> Right = <!RESERVED_SYNTAX_IN_CALLABLE_REFERENCE_LHS!><!DEBUG_INFO_MISSING_UNRESOLVED!>a<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>b<!><<!DEBUG_INFO_MISSING_UNRESOLVED!>Int<!>>.<!DEBUG_INFO_MISSING_UNRESOLVED!>c<!><!>?::<!DEBUG_INFO_MISSING_UNRESOLVED!>foo<!>
}