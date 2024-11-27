// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-73043

class Foo {
    class Nested
    inner class Inner
}

typealias NestedAlias = Foo.Nested
typealias InnerAlias = Foo.Inner

fun test() {
    val foo = Foo()

    val aliasedNested = ::NestedAlias
    val aliasedInner = Foo::InnerAlias

    aliasedNested()
    aliasedInner(foo)
}