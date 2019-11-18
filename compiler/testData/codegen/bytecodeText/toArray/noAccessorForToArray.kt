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

open class SingletonCollection2<T>(val value: T) : AbstractCollection<T>() {
    override val size = 1
    override fun iterator(): Iterator<T> = listOf(value).iterator()
}

// FILE: DerivedSingletonCollection.kt
package test2

import test.*

class DerivedSingletonCollection<T>(value: T) : SingletonCollection<T>(value) {
    fun test() = object {
        fun test() = toArray()
    }.test()

    fun <E> test(a: Array<E>) =  object {
        fun test() =  toArray(a)
    }.test()
}

class DerivedSingletonCollection2<T>(value: T) : SingletonCollection2<T>(value) {
    fun test() = object {
        fun test() = toArray()
    }.test()

    fun <E> test(a: Array<E>) =  object {
        fun test() = toArray(a)
    }.test()

}

// @test/SingletonCollection.class:
// 0 access\$
// 2 public final toArray
// 0 \.toArray

// @test/SingletonCollection2.class:
// 0 access\$
// 0 toArray

// @test2/DerivedSingletonCollection.class:
// 0 access\$
// 0 toArray

// @test2/DerivedSingletonCollection2.class:
// 0 access\$
// 0 toArray
