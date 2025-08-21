// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// ALLOW_KOTLIN_PACKAGE
package kotlin.collections

interface Iterator<out T> {
    operator fun next(): T
    operator fun hasNext(): Boolean
}

interface MutableIterator<out T> : Iterator<T> {
    fun remove(): Unit
}

interface Iterable<out T> {
    operator fun iterator(): Iterator<T>
}

interface MutableIterable<out T> : kotlin.collections.Iterable<T> {
    fun iterator(): MutableIterator<T>
}

interface Collection<out E> : Iterable<E> {
    override fun iterato<caret>r(): Iterator<E>
}