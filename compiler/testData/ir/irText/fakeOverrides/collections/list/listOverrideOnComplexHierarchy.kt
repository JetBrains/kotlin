// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ ISSUE: KT-65219, KT-63914

// FILE: Java1.java
import kotlin.collections.AbstractMutableList;

public abstract class Java1 extends AbstractMutableList { }

// FILE: Java2.java
public interface Java2 {
    public Boolean remove(int i);
}

// FILE: Java3.java
public interface Java3 {
    public Integer remove(int i);
}

// FILE: Java4.java
import java.util.ArrayList;
public class Java4 extends ArrayList<Integer> { }

// FILE: 1.kt
import java.util.*

abstract class A : LinkedList<Int>(), Java2 , MutableCollection<Int> //Kotlin ← Java1, Java2, Kotlin2

class B : A() {
    override fun remove(element: Int): Boolean {
        return true
    }
}

abstract class C: LinkedList<Int>(), KotlinInterface, MutableCollection<Int>    //Kotlin ← Java, Kotlin1, Kotlin2

class D : C() {
    override fun remove(element: Int): Boolean {
        return true
    }
}

abstract class E : Java1(), Java2   //Kotlin ← Java1, Java2 ← Kotlin2

abstract class F : E() {
    override fun remove(element: Int): Boolean {
        return false
    }
}

abstract class G : KotlinInterface2, Java2  //Kotlin ← Java, Kotlin2 ← Kotlin3

abstract class H(override val size: Int) : G() {
    override fun remove(element: Int): Boolean {
        return false
    }
    override fun contains(element: Int): Boolean {
        return false
    }
}

abstract class I : KotlinInterface3, Java3   //Kotlin ← Java, Kotlin2 ← Java2

abstract class J : Java3, Java4() //Kotlin ← Java1, Java2 ← Java3

interface KotlinInterface {
    fun remove(i: Int): Boolean
}

interface KotlinInterface2 : MutableCollection<Int>

interface KotlinInterface3 : java.util.List<Int>

fun test(a: A, b: B, c: C, d: D, e: E, f : F, g: G, h: H, i: I, j: J) {
    a.size
    a.add(1)
    a.add(1,2)
    a.removeAt(1)
    a.remove()
    a.remove(element = 2)

    b.size
    b.add(1)
    b.add(1,2)
    b.removeAt(1)
    b.remove()
    b.remove(2)

    c.size
    c.add(1)
    c.add(1,2)
    c.removeAt(1)
    c.remove()
    c.remove(element = 2)

    d.size
    d.add(1)
    d.add(1,2)
    d.removeAt(1)
    d.remove()
    d.remove(2)

    e.size
    e.add(1)
    e.add(1,2)
    e.removeAt(1)
    e.remove(2)

    f.size
    f.add(1)
    f.add(1,2)
    f.removeAt(1)
    f.remove(2)

    g.size
    g.add(1)
    g.contains(4)
    g.remove(element = 2)

    h.size
    h.add(1)
    h.contains(1)
    h.remove(2)

    i.size
    i.add(1)
    i.add(1,2)
    i.remove(2)

    j.size
    j.add(1)
    j.add(1,2)
    j.get(1)
    j.remove(1)
    j.removeAt(2)
}