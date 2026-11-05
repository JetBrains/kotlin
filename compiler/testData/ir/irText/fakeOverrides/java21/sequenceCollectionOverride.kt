// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// JDK_KIND: FULL_JDK_21
// WITH_STDLIB

// FILE: Java1.java
import java.util.SequencedCollection;
public abstract class Java1 implements SequencedCollection<Integer> { }

// FILE: Java2.java
public interface Java2 {
    int getFirst();
}

// FILE: Java3.java
public abstract class Java3 implements KotlinInterface { }

// FILE: 1.kt
import java.util.*

abstract class A : SequencedCollection<Int>

abstract class B(override val size: Int) :  SequencedCollection<Int> {
    override fun removeFirst(): Int {
        return 1
    }

    override fun reversed(): SequencedCollection<Int> {
        return null!!
    }
}

abstract class C : Java1()  // Kotlin ← Java1 ←Java2

abstract class D : Java1() {
    override val size: Int
        get() = 5

    override fun remove(element: Int?): Boolean {
        return true
    }

    override fun reversed(): SequencedCollection<Int> {
        return null!!
    }

    override fun removeFirst(): Int {
        return 2
    }
}

abstract class E(override val size: Int) : Java1(), Java2 { //Kotlin ← Java1, Java2  ← Java3
    override fun getFirst(): Int {
        return 2
    }
}

abstract class F : KotlinInterface, Java2 { //Kotlin ← Java, Kotlin2 ← Java2
    override fun getFirst(): Int {
        return 2
    }
}

abstract class G : Java3()  //Kotlin ← Java ← Kotlin ← Java

interface KotlinInterface : SequencedCollection<Int>

fun test(a: A, b: B, c: C, d: D, e: E, f: F, g: G) {
    a.addFirst(1)
    a.addLast(null)
    a.first
    a.last
    a.first()
    a.last()
    a.removeFirst()
    a.removeLast()
    a.reversed()

    b.addFirst(1)
    b.addLast(null)
    b.first
    b.last
    b.removeFirst()
    b.removeLast()
    b.reversed()

    c.addFirst(1)
    c.addLast(null)
    c.first
    c.last
    c.removeFirst()
    c.removeLast()
    c.reversed()

    d.removeFirst()
    d.removeLast()
    d.addFirst(1)
    d.addLast(null)
    d.reversed()
    d.first
    d.last

    e.first
    e.last
    e.removeFirst()
    e.removeLast()
    e.addFirst(1)
    e.addLast(null)
    e.reversed()

    f.first
    f.last
    f.removeFirst()
    f.removeLast()
    f.addFirst(1)
    f.addLast(null)
    f.reversed()

    g.first
    g.last
    g.removeFirst()
    g.removeLast()
    g.addFirst(1)
    g.addLast(null)
    g.reversed()
}