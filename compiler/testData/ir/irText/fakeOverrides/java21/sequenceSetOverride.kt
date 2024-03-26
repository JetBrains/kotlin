// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// JDK_KIND: FULL_JDK_21
// WITH_STDLIB
// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ ISSUE: KT-63914, KT-65219

// FILE: Java1.java
import java.util.SequencedSet;

public abstract class Java1 implements SequencedSet<Integer> { }

// FILE: Java2.java
import java.util.SequencedCollection;
public interface Java2 {
    SequencedCollection<Integer> reversed();
}

// FILE: Java3.java
import java.util.SequencedSet;

public abstract class Java3 implements KotlinInterface {

    @Override
    public SequencedSet<Integer> reversed() {
        return null;
    }
}

// FILE: 1.kt
import java.util.*

abstract class A: LinkedHashSet<Int>()

abstract class B(override val size: Int) : LinkedHashSet<Int>() {
    override fun reversed(): LinkedHashSet<Int> {
        return null!!
    }

    override fun addFirst(e: Int?) {
        super.addFirst(e)
    }
}

abstract class C : Java1()   // Kotlin ← Java1 ← Java2

abstract class D(override val size: Int) : Java1() {
    override fun reversed(): SequencedSet<Int> {
        return null!!
    }

    override fun getFirst(): Int {
        return 1
    }
}

abstract class E : Java1(), Java2   //Kotlin ← Java1, Java2  ← Java3

abstract class F(override val size: Int) : Java1(), Java2 {
    override fun reversed(): SequencedSet<Int> {
        return null!!
    }
}

abstract class G: KotlinInterface, Java2    //Kotlin ← Java, Kotlin2 ← Java2

abstract class H(override val size: Int) : KotlinInterface, Java2 {
    override fun reversed(): SequencedSet<Int> {
        return null!!
    }
}

abstract class I : Java3()  //Kotlin ← Java ← Kotlin ← Java

abstract class J : Java3() {
    override fun reversed(): SequencedSet<Int> {
        return null!!
    }
}

interface KotlinInterface : SequencedSet<Int>

fun test(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J) {
    a.reversed()
    a.first
    a.addFirst(1)
    a.addLast(null)

    b.reversed()
    b.first
    b.addFirst(1)
    b.addLast(null)

    c.reversed()
    c.first
    c.addFirst(1)
    c.addLast(null)

    d.reversed()
    d.first
    d.addFirst(1)
    d.addLast(null)

    e.reversed()
    e.first
    e.addFirst(1)
    e.addLast(null)

    f.reversed()
    f.first
    f.addFirst(1)
    f.addLast(null)

    g.reversed()
    g.first
    g.addFirst(1)
    g.addLast(null)

    h.reversed()
    h.first
    h.addFirst(1)
    h.addLast(null)

    i.reversed()
    i.first
    i.addFirst(1)
    i.addLast(null)

    j.reversed()
    j.first
    j.addFirst(1)
    j.addLast(null)
}