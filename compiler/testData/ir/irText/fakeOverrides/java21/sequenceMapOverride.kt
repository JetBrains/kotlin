// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// JDK_KIND: FULL_JDK_21
// WITH_STDLIB

// FILE: Java1.java
import java.util.SequencedMap;
public abstract class Java1 implements SequencedMap<Object, Object> { }

// FILE: Java2.java
public interface Java2 {
    Object putFirst(Object k, Object v);
}

// FILE: Java3.java
public abstract class Java3 implements KotlinInterface  { }

// FILE: 1.kt
import java.util.*

abstract class A : Java1()  // Kotlin ← Java1 ← Java2

abstract class B : Java1() {
    override fun putFirst(k: Any?, v: Any?): Any {
        return ""
    }

    override fun reversed(): SequencedMap<Any, Any>? {
        return null!!
    }

    override fun firstEntry(): MutableMap.MutableEntry<Any, Any> {
        return null!!
    }
}

abstract class C : Java1(), Java2 {     //Kotlin ← Java1, Java2  ← Java3
    override fun putFirst(k: Any?, v: Any?): Any {
        return ""
    }
}

abstract class D : Java2, KotlinInterface {     //Kotlin ← Java, Kotlin2 ← Java2
    override fun putFirst(k: Any?, v: Any?): Any {
        return ""
    }
}

abstract class E : Java3()      //Kotlin ← Java ← Kotlin ← Java

abstract class F(override val size: Int) : Java3() {
    override fun remove(key: Any?): Any? {
        return ""
    }

    override fun reversed(): SequencedMap<Any, Any> {
        return null!!
    }
}

interface KotlinInterface : SequencedMap<Any, Any>

fun test(a: A, b: B, c: C, d: D, e: E, f: F) {
    a.putFirst(1, null)
    a.firstEntry()
    a.putLast(null, 2)
    a.lastEntry()
    a.pollFirstEntry()
    a.reversed()
    a.sequencedKeySet()

    b.putFirst(1, null)
    b.firstEntry()
    b.putLast(null, 2)
    b.lastEntry()
    b.pollFirstEntry()
    b.reversed()
    b.sequencedKeySet()

    c.putFirst(1, null)
    c.firstEntry()
    c.putLast(null, 2)
    c.lastEntry()
    c.reversed()

    d.putFirst(1, null)
    d.firstEntry()
    d.putLast(null, 1)
    d.lastEntry()
    d.reversed()

    e.putFirst(1, null)
    e.firstEntry()
    e.putLast(null, 2)
    e.lastEntry()
    e.reversed()

    f.putFirst(1, null)
    f.firstEntry()
    f.putLast(null, 2)
    f.lastEntry()
    f.reversed()
}