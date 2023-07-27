// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER

interface Foo<T> {
    fun foo(l: List<T>) {}
}

interface Bar<T> {
    fun foo(l: List<T>) {}
}

<!CONFLICTING_INHERITED_JVM_DECLARATIONS!>class Baz(): Foo<String>, Bar<Int> {
}<!>
