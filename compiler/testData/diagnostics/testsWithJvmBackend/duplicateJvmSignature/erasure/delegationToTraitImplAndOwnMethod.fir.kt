// !DIAGNOSTICS: -UNUSED_PARAMETER

interface Foo<T> {
    <!CONFLICTING_JVM_DECLARATIONS!>fun foo(l: List<T>)<!> {

    }
}

class Bar(f: Foo<String>): Foo<String> by f {
    <!CONFLICTING_JVM_DECLARATIONS!>fun foo(l: List<Int>)<!> {}
}

class BarOther(f: Foo<String>): Foo<String> by f {
    override fun foo(l: List<String>) {}
}
