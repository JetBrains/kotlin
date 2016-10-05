// FILE: test/B.java
package test;

public abstract class B<E> extends A<E> implements L<E> {

}

// FILE: main.kt
package test
open class A<T> : Collection<T> {
    override val size: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun contains(element: T): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isEmpty(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun iterator(): Iterator<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

interface L<Q> : List<Q>

// 'remove(Int)' method must be present in C though it has supeclass that is subclass of List
class C<F> : B<F>() {
    override fun get(index: Int): F {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun indexOf(element: F): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun lastIndexOf(element: F): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun listIterator(): ListIterator<F> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun listIterator(index: Int): ListIterator<F> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<F> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
