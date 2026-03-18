// TARGET_BACKEND: JVM

// WITH_STDLIB
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

fun box() = B<String>().toArray(arrayOf("OK"))[0]
