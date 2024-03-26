// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ ISSUE: KT-63914

// FILE: Java1.java
import java.util.ArrayList;
public class Java1<T> extends ArrayList<T>{ }

// FILE: 1.kt
class A<T> : Java1<T>()

class B<T> : Java1<T>() {
    override fun remove(element: T): Boolean {
        return true
    }
    override val size: Int
        get() = 5

    override fun removeAt(index: Int): T {
        return null!!
    }
}

fun test(a: A<Int>, b: B<Int?>) {
    a.size
    a.add(null)
    a.add(1)
    a.add(2,2)
    a.get(1)
    a.remove(1)
    a.removeAt(1)

    b.size
    b.add(null)
    b.add(2)
    b.add(2,2)
    b.get(1)
    b.remove(null)
    b.removeAt(1)
}