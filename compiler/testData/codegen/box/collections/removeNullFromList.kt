// TARGET_BACKEND: JVM
// FILE: removeNullFromList.kt

class MyList : List<String> {
    override val size: Int get() = 0
    override fun contains(element: String): Boolean = false
    override fun containsAll(elements: Collection<String>): Boolean = false
    override fun get(index: Int): String = TODO()
    override fun indexOf(element: String): Int = -1
    override fun isEmpty(): Boolean = true
    override fun iterator(): Iterator<String> = TODO()
    override fun lastIndexOf(element: String): Int = -1
    override fun listIterator(): ListIterator<String> = TODO()
    override fun listIterator(index: Int): ListIterator<String> = TODO()
    override fun subList(fromIndex: Int, toIndex: Int): List<String> = this
}

fun box(): String {
    try {
        J.test(MyList())
    } catch (e: UnsupportedOperationException) {
        return "OK"
    }
    return "J.test(MyList()) should have thrown UnsupportedOperationException"
}

// FILE: J.java
import java.util.List;

public class J {
    public static void test(List<String> list) {
        list.remove(null);
    }
}
