// ISSUE: KT-72390

// We will hopefully be able to remove this test again after KT-68154 has been fixed.

// MODULE: fake-stdlib-common
// ALLOW_KOTLIN_PACKAGE
// STDLIB_COMPILATION

// FILE: Annotations.kt
package kotlin.internal

internal expect annotation class ActualizeByJvmBuiltinProvider()

// FILE: Collections.kt
package kotlin.collections

import kotlin.internal.ActualizeByJvmBuiltinProvider

@ActualizeByJvmBuiltinProvider
public expect interface Iterable<out T> {
    public operator fun iterator(): Iterator<T>
}

@ActualizeByJvmBuiltinProvider
public expect interface Collection<out E> : Iterable<E> {
    public val size: Int
    public fun isEmpty(): Boolean
    public operator fun contains(element: @UnsafeVariance E): Boolean
    override fun iterator(): Iterator<E>
    public fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean
}

@ActualizeByJvmBuiltinProvider
public expect interface MutableCollection<E> : Collection<E>, MutableIterable<E> {
    override fun iterator(): MutableIterator<E>
    public fun add(element: E): Boolean
    public fun remove(element: E): Boolean
    public fun addAll(elements: Collection<E>): Boolean
    public fun removeAll(elements: Collection<E>): Boolean
    public fun retainAll(elements: Collection<E>): Boolean
    public fun clear(): Unit
}

@ActualizeByJvmBuiltinProvider
public expect interface List<out E> : Collection<E> {
    override val size: Int
    override fun isEmpty(): Boolean
    override fun contains(element: @UnsafeVariance E): Boolean
    override fun iterator(): Iterator<E>
    override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean
    public operator fun get(index: Int): E
    public fun indexOf(element: @UnsafeVariance E): Int
    public fun lastIndexOf(element: @UnsafeVariance E): Int
    public fun listIterator(): ListIterator<E>
    public fun listIterator(index: Int): ListIterator<E>
    public fun subList(fromIndex: Int, toIndex: Int): List<E>
}

@ActualizeByJvmBuiltinProvider
public expect interface MutableList<E> : List<E>, MutableCollection<E> {
    override fun add(element: E): Boolean
    override fun remove(element: E): Boolean
    override fun addAll(elements: Collection<E>): Boolean
    public fun addAll(index: Int, elements: Collection<E>): Boolean
    override fun removeAll(elements: Collection<E>): Boolean
    override fun retainAll(elements: Collection<E>): Boolean
    override fun clear(): Unit
    public operator fun set(index: Int, element: E): E
    public fun add(index: Int, element: E): Unit
    public fun removeAt(index: Int): E
    override fun listIterator(): MutableListIterator<E>
    override fun listIterator(index: Int): MutableListIterator<E>
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E>
}

// MODULE: main(fake-stdlib-common)
// FILE: main.kt
package main

// To check whether the actualizing of builtins works, we simply ensure that the `MutableList` symbol is not `expect`. `fake-stdlib-common`
// mocks the `expect` declarations in `stdlib-common`, which are then actualized by the builtins symbol provider.
fun check(l: M<caret>utableList<Int>) { }
