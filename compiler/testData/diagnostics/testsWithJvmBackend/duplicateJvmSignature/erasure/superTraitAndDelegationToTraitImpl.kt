// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER

interface A<T> {
    fun foo(l: List<T>)
}

interface B {
    fun foo(l: List<Int>) {}
}

<!ACCIDENTAL_OVERRIDE!>class C(f: A<String>): A<String> by f, B<!>

<!DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE!>class D<!>(f: A<Int>): A<Int> by f, B
