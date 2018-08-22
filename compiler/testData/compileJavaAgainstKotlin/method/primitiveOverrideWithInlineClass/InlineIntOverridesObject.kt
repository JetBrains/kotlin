// !LANGUAGE: +InlineClasses

package test

inline class Z(val value: Int)

interface IFoo<T> {
    fun foo(): T
}

open class KFooZ : IFoo<Z> {
    override fun foo(): Z = Z(42)
}