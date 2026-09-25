// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB

// K1 kotlin-reflect drops the definitely non-nullable marker (`V & Any`) from the predefined enhancement of `java.util.Map.merge`
// and `computeIfPresent`, while the new implementation matches the compiler
// KOTLIN_REFLECT_DUMP_MISMATCH

// FILE: 1.kt
import java.util.SortedMap

abstract class C<T> : SortedMap<T, T>

abstract class D<T>(
    override val size: Int
) : SortedMap<T, T> {
    override fun remove(key: T): T? {
        return null
    }
}

fun test(c: C<Boolean>, d: D<Boolean>){
    c.size
    c[false] = false
    c[null] = null
    c.remove(null)
    c.remove(true)
    c.get(null)
    c.get(true)

    d.size
    d[false] = false
    d[null] = null
    d.remove(null)
    d.remove(true)
    d.get(null)
    d.get(true)
}