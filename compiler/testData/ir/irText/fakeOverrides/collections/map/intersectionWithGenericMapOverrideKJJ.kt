// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ ISSUE: KT-65667

// FILE: 1.kt
import java.util.HashMap
import java.util.SortedMap

abstract class A<T> : SortedMap<T, T>, HashMap<T, T>()

abstract class B<T> : SortedMap<T, T>, HashMap<T, T>() {
    override fun put(key: T, value: T): T? {
        return null!!
    }

    override fun remove(key: T): T {
        return null!!
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

    b.put(false, false)
    b[true] = true
    b.remove(null)
}