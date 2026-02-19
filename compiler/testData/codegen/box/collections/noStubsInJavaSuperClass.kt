// TARGET_BACKEND: JVM

// FILE: B.java
public abstract class B<E> extends A<E> implements L<E> {
    public String callIndexAdd(int x) {
        add(0, null);
        return null;
    }
}

// FILE: main.kt
open class A<T> : Collection<T> {
    override val size: Int
        get() = TODO("Not yet implemented")

    override fun contains(element: T): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun iterator(): Iterator<T> {
        TODO("Not yet implemented")
    }
}

interface L<Q> : List<Q>

// 'add(Int; Object)' method must be present in C though it has supeclass that is subclass of List
class C<F> : B<F>() {
    override fun get(index: Int): F {
        TODO("Not yet implemented")
    }

    override fun indexOf(element: F): Int {
        TODO("Not yet implemented")
    }

    override fun lastIndexOf(element: F): Int {
        TODO("Not yet implemented")
    }

    override fun listIterator(): ListIterator<F> {
        TODO("Not yet implemented")
    }

    override fun listIterator(index: Int): ListIterator<F> {
        TODO("Not yet implemented")
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<F> {
        TODO("Not yet implemented")
    }
}

fun box() = try {
    C<String>().callIndexAdd(1)
    throw RuntimeException("fail 1")
} catch (e: UnsupportedOperationException) {
    if (e.message != "Operation is not supported for read-only collection") throw RuntimeException("fail 2")
    "OK"
}
