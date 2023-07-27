// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER

interface Foo<T> {
    fun foo(l: List<T>)
}

interface Bar<T> {
    fun foo(l: List<T>)
}

<!CONFLICTING_JVM_DECLARATIONS!>class Baz(f: Foo<String>, b: Bar<Int>) :
    Foo<String> by f,
    Bar<Int> by b {
}<!>
