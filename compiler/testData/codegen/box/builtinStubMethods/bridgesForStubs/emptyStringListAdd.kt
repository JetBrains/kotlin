// TARGET_BACKEND: JVM
// FULL_JDK
// FILE: emptyStringListAdd.kt

object EmptyStringList : List<String> {
    override val size: Int get() = 0
    override fun isEmpty(): Boolean = true
    override fun contains(element: String): Boolean = false
    override fun containsAll(elements: Collection<String>): Boolean = elements.isEmpty()

    override fun get(index: Int): String = null!!
    override fun indexOf(element: String): Int = -1
    override fun lastIndexOf(element: String): Int = -1

    override fun iterator(): Iterator<String> = null!!
    override fun listIterator(): ListIterator<String> = null!!
    override fun listIterator(index: Int): ListIterator<String> = null!!

    override fun subList(fromIndex: Int, toIndex: Int): List<String> = null!!
}

fun box(): String {
    try {
        J.add42(EmptyStringList)
        return "Fail: no exception is thrown from J.add42(list)"
    } catch (e: UnsupportedOperationException) {
        return "OK"
    } catch (e: Throwable) {
        throw AssertionError("Fail: incorrect exception is thrown from J.add42(list)", e)
    }
}

// FILE: J.java
import java.util.*;

public class J {
    public static void add42(List list) {
        list.add(42);
    }
}
