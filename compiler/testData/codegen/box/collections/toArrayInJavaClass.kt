// WITH_RUNTIME
// FILE: B.java
public class B<E> extends A<E> {
    @Override
    public <T> T[] toArray(T[] a) {
        return a;
    }
}

// FILE: main.kt
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

fun box() = B<String>().toArray(arrayOf("OK"))[0]
