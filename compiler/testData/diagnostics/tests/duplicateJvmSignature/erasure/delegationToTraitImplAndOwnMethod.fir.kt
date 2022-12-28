// !DIAGNOSTICS: -UNUSED_PARAMETER

interface Foo<T> {
    fun foo(l: List<T>) {

    }
}

class Bar(f: Foo<String>): Foo<String> by f {
    fun foo(l: List<Int>) {}
}

class BarOther(f: Foo<String>): Foo<String> by f {
    override fun foo(l: List<String>) {}
}