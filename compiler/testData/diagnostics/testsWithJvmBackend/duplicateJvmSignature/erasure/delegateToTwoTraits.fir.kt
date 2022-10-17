// !DIAGNOSTICS: -UNUSED_PARAMETER

interface Foo<T> {
    <!CONFLICTING_JVM_DECLARATIONS!>fun foo(l: List<T>)<!>
}

interface Bar<T> {
    <!CONFLICTING_JVM_DECLARATIONS!>fun foo(l: List<T>)<!>
}

class Baz(f: Foo<String>, b: Bar<Int>) :
    Foo<String> by f,
    Bar<Int> by b {
}
