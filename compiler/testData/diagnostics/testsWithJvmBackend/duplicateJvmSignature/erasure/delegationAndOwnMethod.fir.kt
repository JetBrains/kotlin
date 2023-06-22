// COMPARE_WITH_LIGHT_TREE
// !DIAGNOSTICS: -UNUSED_PARAMETER

interface Foo<T> {
    fun foo(l: List<T>)
}

<!CONFLICTING_JVM_DECLARATIONS{LT}!>class <!CONFLICTING_JVM_DECLARATIONS{PSI}!>Bar(f: Foo<String>)<!>: Foo<String> by f {
    <!CONFLICTING_JVM_DECLARATIONS{LT}!><!CONFLICTING_JVM_DECLARATIONS{PSI}!>fun foo(l: List<Int>)<!> {}<!>
}<!>
