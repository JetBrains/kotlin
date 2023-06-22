// COMPARE_WITH_LIGHT_TREE
// !DIAGNOSTICS: -UNUSED_PARAMETER
interface Foo<X> {
    fun foo(x: X)
}

open class FooImpl : Foo<String> {
    override fun foo(x: String) {
    }
}

open class FooImpl2 : FooImpl() {
    <!ACCIDENTAL_OVERRIDE{LT}!><!ACCIDENTAL_OVERRIDE{PSI}!>fun foo(x: Any)<!> {
    }<!>
}
