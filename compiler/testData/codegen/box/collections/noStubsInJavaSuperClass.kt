// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

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
        get() = throw IllegalStateException("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun contains(element: T): Boolean {
        throw IllegalStateException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        throw IllegalStateException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isEmpty(): Boolean {
        throw IllegalStateException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun iterator(): Iterator<T> {
        throw IllegalStateException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

interface L<Q> : List<Q>

// 'add(Int; Object)' method must be present in C though it has supeclass that is subclass of List
class C<F> : B<F>() {
    override fun get(index: Int): F {
        throw IllegalStateException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun indexOf(element: F): Int {
        throw IllegalStateException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun lastIndexOf(element: F): Int {
        throw IllegalStateException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun listIterator(): ListIterator<F> {
        throw IllegalStateException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun listIterator(index: Int): ListIterator<F> {
        throw IllegalStateException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<F> {
        throw IllegalStateException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

fun box() = try {
    C<String>().callIndexAdd(1)
    throw RuntimeException("fail 1")
} catch (e: UnsupportedOperationException) {
    if (e.message != "Operation is not supported for read-only collection") throw RuntimeException("fail 2")
    "OK"
}
