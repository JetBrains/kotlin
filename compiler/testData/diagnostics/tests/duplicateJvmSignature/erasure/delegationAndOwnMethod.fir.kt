// !DIAGNOSTICS: -UNUSED_PARAMETER

interface Foo<T> {
    fun foo(l: List<T>)
}

class Bar(f: Foo<String>): Foo<String> by f {
    fun foo(l: List<Int>) {}
}