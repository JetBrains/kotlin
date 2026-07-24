// LANGUAGE: +CompanionBlocks +CompanionExtensions
package test

class C {
    companion {
        fun foo() {}
        val bar = 1
        val baz get() = 2
    }

    fun inside() {
        foo()
        bar
        baz
    }
}

fun usage() {
    C.foo()
    C.bar
    C.baz

    C::foo
    C::bar

    test.C.foo()
}
