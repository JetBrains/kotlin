// !DIAGNOSTICS: -UNUSED_PARAMETER
// TARGET_BACKEND: JVM_IR

interface Foo<T> {
    fun foo(l: List<T>)
}

interface Bar<T> {
    fun foo(l: List<T>)
}

class Baz(f: Foo<String>, b: Bar<Int>) :
    Foo<String> by <!CONFLICTING_JVM_DECLARATIONS!>f<!>,
    Bar<Int> by <!CONFLICTING_JVM_DECLARATIONS!>b<!> {
}