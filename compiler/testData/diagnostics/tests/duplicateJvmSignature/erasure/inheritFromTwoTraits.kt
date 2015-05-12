// !DIAGNOSTICS: -UNUSED_PARAMETER

interface Foo<T> {
    fun foo(l: List<T>) {}
}

interface Bar<T> {
    fun foo(l: List<T>) {}
}

class <!CONFLICTING_JVM_DECLARATIONS!>Baz()<!>: Foo<String>, Bar<Int> {
}