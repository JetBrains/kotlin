// WITH_STDLIB
// FULL_JDK

// IGNORE_BACKEND_K2: JVM_IR
// FIR status: KT-57269 K2: collection stub for `sort` is not generated for custom List subclasses

import java.util.*

class ListSet<out E : Any> : List<E>, Set<E> {
    override val size: Int get() = TODO()
    override fun isEmpty(): Boolean = TODO()
    override fun get(index: Int): E = TODO()
    override fun contains(element: @UnsafeVariance E): Boolean = TODO()
    override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean = TODO()
    override fun indexOf(element: @UnsafeVariance E): Int = TODO()
    override fun lastIndexOf(element: @UnsafeVariance E): Int = TODO()
    override fun iterator(): Iterator<E> = TODO()
    override fun listIterator(): ListIterator<E> = TODO()
    override fun listIterator(index: Int): ListIterator<E> = TODO()
    override fun spliterator(): Spliterator<@UnsafeVariance E> = TODO()
    override fun subList(fromIndex: Int, toIndex: Int): List<E> = TODO()
}
