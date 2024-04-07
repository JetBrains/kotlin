// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ ISSUE: KT-63914, KT-65667

// FILE: Java1.java
import java.util.HashMap;

public abstract class Java1 extends HashMap<Void, Void> { }

// FILE: 1.kt
abstract class A : Java1()  //Kotlin ← Java1 ←Java2

class B(override val size: Int) : Java1() {
    override fun get(key: Void?): Void? {
        return null
    }

    override fun put(key: Void?, value: Void?): Void? {
        return null
    }
}

fun test(a: A, b: B) {
    a.size
    a.get(null)
    a.remove(null)
    a.put(null, null)
    a.set(null, null)

    b.size
    b.get(null)
    b.remove(null)
    b.put(null, null)
    b.set(null, null)
}