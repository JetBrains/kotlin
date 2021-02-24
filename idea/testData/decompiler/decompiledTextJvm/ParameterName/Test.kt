@file:JvmName("ParameterName")
package test

class Foo {
    companion object
}

class Context<T, U>

inline fun <A, B, C : Any, D> Foo.Companion.foo(crossinline block: Context<B, C>.(input: A, state: B) -> D) {}