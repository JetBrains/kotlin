// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB

// FILE: 1.kt
import java.util.SortedMap

abstract class A : SortedMap<Boolean, Boolean>

abstract class B(override val values: MutableCollection<Boolean>, override val size: Int) : SortedMap<Boolean, Boolean> {
    override fun remove(key: Boolean?): Boolean? {
        return true
    }

    override fun get(key: Boolean?): Boolean? {
        return true
    }
}


fun test(a: A, b: B) {
    a.size
    a[true] = true
    a[null] = null
    a.get(true)
    a.get(null)
    a.remove(null)
    a.remove(true)

    b.size
    b.values
    b[false] = false
    b[null] = null
    b.remove(null)
    b.remove(true)
    b.get(null)
    b.get(true)
}