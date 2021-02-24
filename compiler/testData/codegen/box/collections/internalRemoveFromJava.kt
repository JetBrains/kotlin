// TARGET_BACKEND: JVM

// IGNORE_BACKEND: JVM
// ^ KT-43334 AbstractMethodError when calling 'remove' from Java on a Kotlin Collection with custom internal 'remove'
//   fixed in JVM_IR

// FILE: internalRemoveFromJava.kt

class Test<T> : Collection<T> {
    override val size: Int get() = TODO()
    override fun contains(element: T): Boolean = TODO()
    override fun containsAll(elements: Collection<T>): Boolean = TODO()
    override fun isEmpty(): Boolean = TODO()
    override fun iterator(): Iterator<T> = TODO()

    internal fun remove(x: T): Boolean = false
}

fun box(): String {
    val t = Test<String>()
    return if (J.testRemove(t, "") == false)
        "OK"
    else
        "Fail"
}

// FILE: J.java
import java.util.Collection;

public class J {
    public static <T> boolean testRemove(Collection<T> c, T x) {
        return c.remove(x);
    }
}
