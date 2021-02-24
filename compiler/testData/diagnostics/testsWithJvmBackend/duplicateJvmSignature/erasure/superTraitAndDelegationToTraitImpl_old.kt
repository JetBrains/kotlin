// !DIAGNOSTICS: -UNUSED_PARAMETER
// TARGET_BACKEND: JVM_OLD

interface A<T> {
    fun foo(l: List<T>)
}

interface B {
    fun foo(l: List<Int>) {}
}

class <!CONFLICTING_INHERITED_JVM_DECLARATIONS, CONFLICTING_JVM_DECLARATIONS!>C(f: A<String>)<!>: A<String> by f, B

<!DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE!>class D<!>(f: A<Int>): A<Int> by f, B