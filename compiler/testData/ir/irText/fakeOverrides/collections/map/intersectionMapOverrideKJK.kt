// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB

// FILE: 1.kt
import java.util.SortedMap

abstract class A : SortedMap<Boolean, Boolean>, Map<Boolean, Boolean>

abstract class B : SortedMap<Boolean, Boolean>, Map<Boolean, Boolean> {
    override fun put(key: Boolean, value: Boolean): Boolean? {
        return false
    }

    override fun remove(key: Boolean?): Boolean? {
        return false
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

    b.put(false, false)
    b[true] = true
    b.remove(null)
}