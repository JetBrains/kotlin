// RUN_PIPELINE_TILL: FRONTEND
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
    val aliasedInner = Foo::<!EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED!>InnerAlias<!>

    aliasedNested()
    aliasedInner(foo)
}
