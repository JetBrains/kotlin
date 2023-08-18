// FIR_IDENTICAL
// ISSUE: KT-60269
// WITH_STDLIB

interface Foo {
    fun foo(param: Int = 1)
}

interface Bar {
    fun foo(param: Int = 2)
}

class Baz : Bar, Foo {
    override fun foo(<!MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES!>param: Int<!>) {
        println(param)
    }
}

fun main() {
    Baz().foo()
    (Baz() as Foo).foo()
}
