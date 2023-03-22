// IGNORE_REVERSED_RESOLVE
// FIR_IDENTICAL
// !LANGUAGE: +InlineClasses, -JvmInlineValueClasses
// FILE: kt1.kt
package kt

inline class Z(val value: Int)

interface IFoo<T> {
    fun foo(): T
}

open class KFooZ : IFoo<Z> {
    override fun foo(): Z = Z(42)
}

// FILE: j/J.java
package j;

import kt.Z;
import kt.KFooZ;

public class J extends KFooZ {
}

// FILE: kt2.kt
package kt

import j.J

fun jfoo(x: J) = x.foo()