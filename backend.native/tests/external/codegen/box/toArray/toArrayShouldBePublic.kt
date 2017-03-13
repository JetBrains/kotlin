// TARGET_BACKEND: JVM
// WITH_RUNTIME

// FILE: SingletonCollection.kt
package test

open class SingletonCollection<T>(val value: T) : AbstractCollection<T>() {
    override val size = 1
    override fun iterator(): Iterator<T> = listOf(value).iterator()

    protected override final fun toArray(): Array<Any?> =
            arrayOf<Any?>(value)

    protected override final fun <E> toArray(a: Array<E>): Array<E> {
        a[0] = value as E
        return a
    }
}

// FILE: DerivedSingletonCollection.kt
package test2

import test.*

class DerivedSingletonCollection<T>(value: T) : SingletonCollection<T>(value)

// FILE: box.kt
import test.*
import test2.*

fun box(): String {
    val sc = SingletonCollection(42)

    val test1 = (sc as java.util.Collection<Int>).toArray()
    if (test1[0] != 42) return "Failed #1"

    val test2 = arrayOf<Any?>(0)
    (sc as java.util.Collection<Int>).toArray(test2)
    if (test2[0] != 42) return "Failed #2"

    val dsc = DerivedSingletonCollection(42)
    val test3 = (dsc as java.util.Collection<Int>).toArray()
    if (test3[0] != 42) return "Failed #3"

    val test4 = arrayOf<Any?>(0)
    (dsc as java.util.Collection<Int>).toArray(test4)
    if (test4[0] != 42) return "Failed #4"

    return "OK"
}