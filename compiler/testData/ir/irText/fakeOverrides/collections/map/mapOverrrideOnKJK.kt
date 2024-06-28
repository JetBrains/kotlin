// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB

// FILE: Java1.java
import kotlin.collections.AbstractMutableMap;

abstract public class Java1 extends AbstractMutableMap<Object, Object>{}

// FILE: 1.kt
abstract class A : Java1()

class B(override val entries: MutableSet<MutableMap.MutableEntry<Any, Any>>) : Java1() {
    override fun put(key: Any?, value: Any?): Any? {
        return null
    }
}

fun test(a: A, b: B) {
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