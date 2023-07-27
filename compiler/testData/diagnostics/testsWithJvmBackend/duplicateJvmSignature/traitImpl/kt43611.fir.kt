// !DIAGNOSTICS: -UNUSED_PARAMETER

interface A {
    <!CONFLICTING_JVM_DECLARATIONS!>fun f(a: List<Int>): String = TODO()<!>
    <!CONFLICTING_JVM_DECLARATIONS!>private fun f(a: List<String>): String = TODO()<!>
}

class B : A
