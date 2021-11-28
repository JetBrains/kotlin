fun box(): String {
    return "OK"
}

fun foo() {
    foldingUnary(null)

    foldingBinary(null, null)

    foldingBuiltinBinary(null, null)
}

class Foo {
    fun foo() {}
    fun foo(foo: Foo?) {}
}

inline fun foldingUnary(foo: Foo?) {
    foo!!
    foo.foo()
}

inline fun foldingBinary(foo1: Foo?, foo2: Foo?) {
    foo1!!
    foo2!!
    foo1.foo(foo2)
}

inline fun foldingBuiltinBinary(int1: Int?, int2: Int?) {
    int1!!
    int2!!
    int1 < int2
}