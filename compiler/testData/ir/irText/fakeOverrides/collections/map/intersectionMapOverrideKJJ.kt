// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ ISSUE: KT-65219, KT-63914

// FILE: 1.kt
import java.util.HashMap
import java.util.SortedMap

abstract class A : SortedMap<Boolean, Boolean>, HashMap<Boolean, Boolean>()

abstract class B : SortedMap<Boolean, Boolean>, HashMap<Boolean, Boolean>() {
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