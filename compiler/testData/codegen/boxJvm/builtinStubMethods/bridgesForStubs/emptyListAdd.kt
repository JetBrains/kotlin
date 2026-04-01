// TARGET_BACKEND: JVM
// FILE: emptyListAdd.kt

object EmptyList : List<Nothing>, RandomAccess {
    override val size: Int get() = 0
    override fun isEmpty(): Boolean = true
    override fun contains(element: Nothing): Boolean = false
    override fun containsAll(elements: Collection<Nothing>): Boolean = elements.isEmpty()

    override fun get(index: Int): Nothing = null!!
    override fun indexOf(element: Nothing): Int = -1
    override fun lastIndexOf(element: Nothing): Int = -1

    override fun iterator(): Iterator<Nothing> = null!!
    override fun listIterator(): ListIterator<Nothing> = null!!
    override fun listIterator(index: Int): ListIterator<Nothing> = null!!

    override fun subList(fromIndex: Int, toIndex: Int): List<Nothing> = null!!
}

fun box(): String {
    try {
        J.add()
        return "Fail: no exception is thrown from J.add()"
    } catch (e: UnsupportedOperationException) {
        return "OK"
    } catch (e: Throwable) {
        throw AssertionError("Fail: incorrect exception is thrown from J.add()", e)
    }
}

// FILE: J.java
public class J {
    public static void add() {
        EmptyList.INSTANCE.add("");
    }
}
