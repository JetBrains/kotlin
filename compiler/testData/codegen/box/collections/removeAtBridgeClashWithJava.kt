// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: removeAtBridgeClashWithJava.kt

abstract class AJALI : JavaAbstractList<Int>()

class K : AJALI() {
    override val size: Int get() = TODO()
    override fun contains(element: Int?): Boolean = TODO()
    override fun containsAll(elements: Collection<Int>): Boolean = TODO()
    override fun get(index: Int): Int = TODO()
    override fun indexOf(element: Int?): Int = TODO()
    override fun isEmpty(): Boolean = TODO()
    override fun iterator(): MutableIterator<Int> = TODO()
    override fun lastIndexOf(element: Int?): Int = TODO()
    override fun add(element: Int?): Boolean = TODO()
    override fun add(index: Int, element: Int?): Unit = TODO()
    override fun addAll(index: Int, elements: Collection<Int>): Boolean = TODO()
    override fun addAll(elements: Collection<Int>): Boolean = TODO()
    override fun clear(): Unit = TODO()
    override fun listIterator(): MutableListIterator<Int> = TODO()
    override fun listIterator(index: Int): MutableListIterator<Int> = TODO()
    override fun remove(element: Int?): Boolean = TODO()
    override fun removeAll(elements: Collection<Int>): Boolean = TODO()
    override fun retainAll(elements: Collection<Int>): Boolean = TODO()
    override fun set(index: Int, element: Int?): Int = TODO()
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<Int> = TODO()
}

fun box(): String {
    K().removeAt(32)
    return JavaAbstractList.OK
}


// FILE: JavaAbstractList.java
import java.util.List;

public abstract class JavaAbstractList<T> implements List<T> {
    public static String OK = "";

    @Override
    public final T remove(int index) {
        OK = "OK";
        return null;
    }
}
