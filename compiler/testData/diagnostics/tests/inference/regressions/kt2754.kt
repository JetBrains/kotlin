// FIR_IDENTICAL
// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER

// T is the immutable type
interface Builder<out T> {
    fun build(): T
}

// T is the immutable type, U is the builder
interface Copyable<out T, out U : Builder<T>> {
    fun builder(): U
}

fun <T : Copyable<T, U>, U : Builder<T>> T.copy(fn: U.() -> Unit): T = throw Exception()

open class Foo(val x: Int, val y: Int) : Copyable<Foo, Foo.FooBuilder> {
    override fun builder(): FooBuilder = FooBuilder(x, y)

    open class FooBuilder(var x: Int, var y: Int): Builder<Foo> {
        override fun build(): Foo = Foo(x, y)
    }
}

fun test() {
    val foo1 = Foo(x = 1, y = 2)
    val foo2 = foo1.copy { y = 3 } // this doesn't work
    foo2 checkType { _<Foo>() }
}