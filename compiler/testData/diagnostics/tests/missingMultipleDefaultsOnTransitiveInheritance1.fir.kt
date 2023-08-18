// ISSUE: KT-60269
// WITH_STDLIB

interface Foo {
    fun foo(param: Int = 1)
}

interface Bar {
    fun foo(param: Int = 2)
}

interface Baz1 : Bar

class Baz : Baz1, Foo {
    override fun foo(<!MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_DEPRECATION_WARNING!>param: Int<!>) {
        println(param)
    }
}

fun main() {
    Baz().foo()
    (Baz() as Foo).foo()
}