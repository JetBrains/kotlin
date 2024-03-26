// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ ISSUE: KT-65667

// FILE: Java1.java
public interface Java1 {
    Boolean remove(Object key);
}

// FILE: Java2.java
import java.util.SortedMap;
public abstract class Java2 implements SortedMap<Boolean, Boolean> { }

// FILE: Java3.java
import kotlin.collections.AbstractMutableMap;
public abstract class Java3 extends AbstractMutableMap<Boolean, Boolean> { }

// FILE: 1.kt
import java.util.*

abstract class A : SortedMap<Boolean, Boolean>, HashMap<Boolean, Boolean>(), Java1  //Kotlin ← Java1, Java2, Java3

abstract class B : SortedMap<Boolean, Boolean>, HashMap<Boolean, Boolean>(), Java1 {
    override fun remove(key: Boolean): Boolean? {
        return false
    }
}

abstract class C : SortedMap<Boolean, Boolean>, HashMap<Boolean, Boolean>(), KotlinInterface    //Kotlin ← Java1, Java2, Kotlin2

abstract class D : SortedMap<Boolean, Boolean>, HashMap<Boolean, Boolean>(), KotlinInterface {
    override fun remove(key: Boolean): Boolean {
        return true
    }
}

abstract class E : SortedMap<Boolean, Boolean>, MutableMap<Boolean, Boolean>, KotlinInterface   //Kotlin ← Java, Kotlin1, Kotlin2

abstract class F : SortedMap<Boolean, Boolean>, MutableMap<Boolean, Boolean>, KotlinInterface {
    override fun remove(key: Any): Boolean {
        return true
    }
}

abstract class I : Java1, Java2()   // Kotlin ← Java1, Java2 ← Java3

abstract class J : Java1, Java2() {
    override fun remove(key: Boolean?): Boolean? {
        return true
    }
}

abstract class K : Java1, Java3()   //Kotlin ← Java1, Java2 ← Kotlin2

abstract class L : Java1, Java3() {
    override fun remove(key: Boolean): Boolean {
        return true
    }
}

abstract class M : Java1, KotlinInterface3  //Kotlin ← Java, Kotlin2 ← Kotlin3

abstract class N : Java1, KotlinInterface3 {
    override fun remove(key: Boolean): Boolean? {
        return null
    }
}


interface KotlinInterface {
    fun remove(key: Any): Boolean
}

interface KotlinInterface2 : SortedMap<Boolean, Boolean>

interface KotlinInterface3 : MutableMap<Boolean, Boolean>

fun test(a: A, b: B, c: C, d: D, e: E, f: F, i: I, j: J, k: K, l: L, m: M, n: N) {
    a.size
    a.remove(null)
    a.remove(true)

    b.size
    b.remove(null)
    b.remove(true)

    c.size
    c.remove(null)
    c.remove(true)
    c.remove("")

    d.size
    d.remove(true)
    d.remove("")

    e.size
    e.remove(true)
    e.remove("")
    e.remove(null)

    f.size
    f.remove(true)
    f.remove("")
    f.remove(null)

    i.size
    i.remove(null)
    i.remove(true)
    i.remove("")

    j.size
    j.remove(null)
    j.remove(true)
    j.remove("")

    k.size
    k.remove(null)
    k.remove(true)
    k.remove("")

    l.size
    l.remove(null)
    l.remove(true)
    l.remove("")

    m.size
    m.remove(null)
    m.remove(true)
    m.remove("")

    n.size
    n.remove(null)
    n.remove(true)
    n.remove("")
}