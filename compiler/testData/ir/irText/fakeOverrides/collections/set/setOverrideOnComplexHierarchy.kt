// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ ISSUE: KT-65219, KT-63914

// FILE: Java1.java
public interface Java1 {
    boolean remove(Object o);
}

// FILE: Java2.java
import kotlin.collections.AbstractMutableSet;
public abstract class Java2 extends AbstractMutableSet<Integer> { }

// FILE: Java3.java
import java.util.HashSet;
public class Java3 extends HashSet<Integer> { }

// FILE: 1.kt
import java.util.*;
import kotlin.collections.HashSet

abstract class A : SortedSet<Int> , Java1, MutableSet<Int>  //Kotlin ← Java1, Java2, Kotlin2

abstract class B(override val size: Int) : SortedSet<Int> , Java1, MutableSet<Int> {
    override fun add(element: Int): Boolean {
        return true
    }
    override fun remove(element: Int): Boolean {
        return true
    }
}

abstract class C: Java1, MutableSet<Int>, HashSet<Int>()    //Kotlin ← Java, Kotlin1, Kotlin2

class D: Java1, MutableSet<Int>, HashSet<Int>() {
    override fun remove(o: Any?): Boolean {
        return true
    }
}

abstract class E : Java1, SortedSet<Int>, LinkedHashSet<Int>() {    //Kotlin ← Java1, Java2, Java3
    override fun remove(o: Any?): Boolean {
        return true
    }
    override fun spliterator(): Spliterator<Int> {
        return null!!
    }
}

abstract class F : Java1, Java2()    //Kotlin ← Java1, Java2 ← Kotlin2

abstract class G(override val size: Int) : Java1, Java2() {
    override fun add(element: Int?): Boolean {
        return true
    }
    override fun remove(o: Any?): Boolean {
        return true
    }
}

abstract class H : Java1, KotlinClass() {   //Kotlin ← Java, Kotlin2 ← Kotlin3
    override fun remove(element: Int): Boolean {
        return true
    }
}

abstract class I : KotlinInterface, Java1  //Kotlin ← Java, Kotlin2 ← Java2

abstract class J : Java3(), Java1   //Kotlin ← Java1, Java2 ← Java3

open class KotlinClass : HashSet<Int>() {
    override fun remove(element : Int): Boolean {
        return  true
    }
}

interface KotlinInterface : SortedSet<Int>

fun test(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J) {
    a.size
    a.first()
    a.remove(null)
    a.remove(1)
    a.add(null)
    a.add(1)

    b.size
    b.first()
    b.remove(1)
    b.add(1)

    c.size
    c.remove(1)
    c.add(1)

    d.size
    d.add(1)
    d.remove(1)

    e.size
    e.first()
    e.remove(null)
    e.remove(1)
    e.add(1)
    e.add(null)

    f.size
    f.add(1)
    f.add(null)
    f.remove(1)
    f.remove(null)

    g.size
    g.add(null)
    g.add(1)
    g.remove(null)
    g.remove(1)

    h.size
    h.add(1)
    h.remove(1)
    h.remove(null)

    i.size
    i.add(null)
    i.add(1)
    i.remove(1)
    i.remove(null)

    j.size
    j.add(null)
    j.add(1)
    j.remove(null)
    j.remove(1)
}