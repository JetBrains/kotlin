// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: removeClashJava.kt
class Queue<T>() : Collection<T> {
    override val size: Int = 1
    override fun contains(element: T): Boolean = TODO()
    override fun containsAll(elements: Collection<T>): Boolean = TODO()
    override fun isEmpty(): Boolean = TODO()
    override fun iterator(): Iterator<T> = TODO()

    fun remove(v: Any?): Any? = v
}

fun box(): String {
    val q = Queue<String>()
    J.testRemove(q)
    return q.remove("OK") as String
}

// FILE: J.java
import java.util.Collection;

public class J {
    public static void testRemove(Collection<String> c) {
        try {
            c.remove("");
            throw new AssertionError("c.remove(...) should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        } catch (Throwable e) {
            throw new AssertionError("c.remove(...) should throw UnsupportedOperationException");
        }
    }
}