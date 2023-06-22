// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER
// TARGET_BACKEND: JVM_IR

interface A<T> {
    fun foo(l: List<T>)
}

interface B {
    fun foo(l: List<Int>) {}
}

class <!ACCIDENTAL_OVERRIDE!>C(f: A<String>)<!>: A<String> by f, B

<!DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE!>class D<!>(f: A<Int>): A<Int> by f, B
