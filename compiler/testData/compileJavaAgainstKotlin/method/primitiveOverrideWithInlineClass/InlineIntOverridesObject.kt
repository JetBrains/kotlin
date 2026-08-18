// IGNORE_FIR
// KT-64909
// LANGUAGE: +InlineClasses

// FILE: InlineIntOverridesObject.java
package test;

class JExtendsKFooZ extends KFooZ {
}

// FILE: InlineIntOverridesObject.kt
package test

inline class Z(val value: Int)

interface IFoo<T> {
    fun foo(): T
}

open class KFooZ : IFoo<Z> {
    override fun foo(): Z = Z(42)
}
