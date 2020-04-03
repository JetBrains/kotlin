// !DIAGNOSTICS: -UNUSED_PARAMETER
// TARGET_BACKEND: JVM_OLD

interface Foo<T> {
    fun foo(l: List<T>) {}
}

interface Bar<T> {
    fun foo(l: List<T>) {}
}

class <!CONFLICTING_INHERITED_JVM_DECLARATIONS, CONFLICTING_JVM_DECLARATIONS!>Baz()<!>: Foo<String>, Bar<Int> {
}