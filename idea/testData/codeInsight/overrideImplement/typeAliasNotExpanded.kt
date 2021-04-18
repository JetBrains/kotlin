// FIR_IDENTICAL
typealias Foo = Int

interface Bar {
    fun test(foo: Foo) = Unit
}

class Bar2 : Bar {
    <caret>
}