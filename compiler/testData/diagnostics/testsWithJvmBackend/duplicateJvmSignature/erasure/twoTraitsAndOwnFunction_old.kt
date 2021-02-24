// !DIAGNOSTICS: -UNUSED_PARAMETER
// TARGET_BACKEND: JVM_OLD

interface Foo<T> {
    fun foo(l: List<T>) {}
}

interface Bar<T> {
    fun foo(l: List<T>) {}
}

class <!CONFLICTING_JVM_DECLARATIONS!>Baz()<!>: Foo<String>, Bar<Int> {
    <!ACCIDENTAL_OVERRIDE, CONFLICTING_JVM_DECLARATIONS!>fun foo(l: List<Long>)<!> {}
}