// NamedFunctionDescriptor.substitute substitutes "overrides"
// this test checks it does it properly

trait Foo<P> {
    fun quux(p: P, q: Int = 17) : Int = 18
}

trait Bar<Q> : Foo<Q>

abstract class Baz() : Bar<String>

fun zz(b: Baz) = b.quux("a")
