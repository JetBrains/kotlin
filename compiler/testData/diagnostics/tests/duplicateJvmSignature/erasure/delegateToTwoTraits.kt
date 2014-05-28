// !DIAGNOSTICS: -UNUSED_PARAMETER

trait Foo<T> {
    fun foo(l: List<T>)
}

trait Bar<T> {
    fun foo(l: List<T>)
}

<!CONFLICTING_JVM_DECLARATIONS!>class Baz(f: Foo<String>, b: Bar<Int>)<!>: Foo<String> by f, Bar<Int> by b {
}