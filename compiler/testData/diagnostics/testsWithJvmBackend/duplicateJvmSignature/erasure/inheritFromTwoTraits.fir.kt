// COMPARE_WITH_LIGHT_TREE
// !DIAGNOSTICS: -UNUSED_PARAMETER

interface Foo<T> {
    fun foo(l: List<T>) {}
}

interface Bar<T> {
    fun foo(l: List<T>) {}
}

<!CONFLICTING_INHERITED_JVM_DECLARATIONS{LT}!>class <!CONFLICTING_INHERITED_JVM_DECLARATIONS{PSI}!>Baz()<!>: Foo<String>, Bar<Int> {
}<!>
