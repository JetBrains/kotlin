// COMPARE_WITH_LIGHT_TREE
// !DIAGNOSTICS: -UNUSED_PARAMETER

interface Foo<T> {
    fun foo(l: List<T>) {}
}

interface Bar<T> {
    fun foo(l: List<T>) {}
}

class Baz(): Foo<String>, Bar<Int> {
    <!ACCIDENTAL_OVERRIDE{LT}!><!ACCIDENTAL_OVERRIDE{PSI}!>fun foo(l: List<Long>)<!> {}<!>
}
