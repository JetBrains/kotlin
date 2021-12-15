// TARGET_BACKEND: JVM

// MODULE: lib
// FILE: RandomAccessList.kt

abstract class RandomAccessList<T> : List<T>, RandomAccess

// MODULE: main(lib)
// FILE: derivedEmptyListSeveralModulesAdd.kt

open class EmptyListBase<T> : RandomAccessList<T>() {
    override val size: Int get() = 0
    override fun isEmpty(): Boolean = true
    override fun contains(element: T): Boolean = false
    override fun containsAll(elements: Collection<T>): Boolean = elements.isEmpty()

    override fun get(index: Int): T = null!!
    override fun indexOf(element: T): Int = -1
    override fun lastIndexOf(element: T): Int = -1

    override fun iterator(): Iterator<T> = null!!
    override fun listIterator(): ListIterator<T> = null!!
    override fun listIterator(index: Int): ListIterator<T> = null!!

    override fun subList(fromIndex: Int, toIndex: Int): List<T> = null!!
}

object EmptyList : EmptyListBase<Nothing>()

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
