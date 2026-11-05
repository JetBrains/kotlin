// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB

// FILE: Java1.java
abstract public class Java1 extends KotlinClass {}

// FILE: 1.kt
import java.util.SortedMap

abstract class A : Java1()

abstract class B : Java1() {
    override val size: Int
        get() = 5

    override fun get(key: Boolean?): Boolean? {
        return true
    }

    override fun isEmpty(): Boolean {
        return true
    }
}

abstract class KotlinClass : SortedMap<Boolean, Boolean>

fun test(a: A, b: B) {
    a.size
    a[true] = true
    a.put(null, null)
    a.get(true)
    a.get(null)
    a.remove(null)
    a.remove(true)
    a.isNotEmpty()

    b.size
    b.put(false, false)
    b.put(null, null)
    b[null] = null
    b[true] = true
    b.get(null)
    b.get(true)
    b.remove(null)
    b.remove(true)
    b.isEmpty()
}