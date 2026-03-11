// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: kt40305.kt

class ListImpl<A>(private val list: List<A>): List<A> {
    override val size: Int get() = list.size
    override fun contains(element: A): Boolean = list.contains(element)
    override fun containsAll(elements: Collection<A>): Boolean = list.containsAll(elements)
    override fun get(index: Int): A = list.get(index)
    override fun indexOf(element: A): Int = list.indexOf(element)
    override fun isEmpty(): Boolean = list.isEmpty()
    override fun iterator(): Iterator<A> = list.iterator()
    override fun lastIndexOf(element: A): Int = list.lastIndexOf(element)
    override fun listIterator(): ListIterator<A> = list.listIterator()
    override fun listIterator(index: Int): ListIterator<A> = list.listIterator(index)
    override fun subList(fromIndex: Int, toIndex: Int): List<A> = list.subList(fromIndex, toIndex)
}

fun box(): String {
    try {
        J.testAddAllNull(ListImpl(listOf<String>()))
    } catch (e: UnsupportedOperationException) {
        return "OK"
    }
    return "J.testAddAllNull(ListImpl(...)) should throw UnsupportedOperationException"
}

// FILE: J.java
import java.util.List;

public class J {
    public static <T> void testAddAllNull(List<T> list) {
        list.addAll(null);
    }
}
