// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ ISSUE: KT-65219, KT-63914

// FILE: Java1.java
import java.util.ArrayList;
public class Java1 extends ArrayList<Integer>{ }

// FILE: 1.kt
class A : Java1()

class B : Java1() {
    override fun remove(element: Int?): Boolean {
        return true
    }
    override val size: Int
        get() = 5

    override fun removeAt(index: Int): Int {
        return 1
    }
}

fun test(a: A, b: B) {
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