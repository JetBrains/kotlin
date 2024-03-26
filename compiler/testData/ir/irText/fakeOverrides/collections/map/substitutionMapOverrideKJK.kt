// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB

// FILE: Java1.java
import kotlin.collections.AbstractMutableMap;

abstract public class Java1<T> extends AbstractMutableMap<T, T>{}

// FILE: 1.kt
abstract class A<T> : Java1<T>()

class B<T>(override val entries: MutableSet<MutableMap.MutableEntry<T, T>>) : Java1<T>() {
    override fun put(key: T, value: T): T? {
        return null
    }
}

fun test(a: A<Boolean>, b: B<Boolean?>) {
    a.size
    a[true] = true
    a.put(null, null)
    a.get(true)
    a.get(null)
    a.remove(null)
    a.remove(true)

    b.size
    b.put(false, false)
    b.put(null, null)
    b[null] = null
    b[true] = true
    b.get(null)
    b.get(true)
    b.remove(null)
    b.remove(true)
}