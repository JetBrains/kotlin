// !DIAGNOSTICS: -UNUSED_PARAMETER
// TARGET_BACKEND: JVM_IR

interface Foo<T> {
    fun foo(l: List<T>)
}

class Bar(f: Foo<String>): Foo<String> by <!CONFLICTING_JVM_DECLARATIONS!>f<!> {
    <!CONFLICTING_JVM_DECLARATIONS!>fun foo(l: List<Int>)<!> {}
}