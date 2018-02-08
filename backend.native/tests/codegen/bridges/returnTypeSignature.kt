package codegen.bridges.signature

import kotlin.test.*

class A { }

class B { }

class C { }

interface Parser<in IN: Any, out OUT: Any> {
    fun parse(source: IN): OUT
}

interface MultiParser<in IN: Any, out OUT: Any> {
    fun parse(source: IN): Collection<OUT>
}

interface ExtendsInterface<T: Any>: Parser<A, T>, MultiParser<B, T> {
    override fun parse(source: B): Collection<T> = ArrayList<T>()
}

abstract class AbstractClass(): ExtendsInterface<C> {
    public override fun parse(source: A): C = C()
}

@Test fun runTest() {
    val array = object : AbstractClass() { }.parse(B())
}