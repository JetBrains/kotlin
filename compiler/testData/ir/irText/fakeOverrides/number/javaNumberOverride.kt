// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// MUTE_SIGNATURE_COMPARISON_K2: JVM_IR

// FILE: Java1.java
public abstract class Java1 extends Number { }

// FILE: Java2.java
public interface Java2  {
    default int intValue(){
        return 2;
    }
}

// FILE: Java3.java
public class Java3 extends Number {
    @Override
    public int intValue() {
        return 0;
    }

    @Override
    public long longValue() {
        return 2;
    }

    @Override
    public float floatValue() {
        return 3;
    }

    @Override
    public double doubleValue() {
        return 4;
    }
}
// FILE: Java4.java
public abstract class Java4 extends A { }

// FILE: Java5.java
public class Java5 extends A {
    @Override
    public int intValue() {
        return 0;
    }

    @Override
    public long longValue() {
        return 1;
    }

    @Override
    public float floatValue() {
        return 2;
    }

    @Override
    public double doubleValue() {
        return 3;
    }
}

// FILE: 1.kt
abstract class A : java.lang.Number()   //Kotlin ← Java

class B : java.lang.Number() {
    override fun intValue(): Int {
        return 1
    }
    override fun longValue(): Long {
        return 2
    }
    override fun floatValue(): Float {
        return 3.0F
    }
    override fun doubleValue(): Double {
        return 4.0
    }
}

abstract class C : java.lang.Number(), Java2    //Kotlin ← Java1, Java2

abstract class D : java.lang.Number(), Java2 {
    override fun intValue(): Int {
        return 1
    }
    override fun longValue(): Long {
        return 2
    }
}

abstract class E : java.lang.Number(), KotlinInterface  //Kotlin ← Java, Kotlin2

abstract class F : java.lang.Number(), KotlinInterface {
    override fun floatValue(): Float {
        return 3.0F
    }
    override fun doubleValue(): Double {
        return 4.0
    }
}

abstract class G : Java1()  //Kotlin ← Java1 ← Java2

abstract class H : Java1() {
    override fun toByte(): Byte {
        return 1
    }
    override fun toDouble(): Double {
        return 1.0
    }
}

abstract class I : Java3()  //Kotlin ← Java1 (override) ← Java2

class J : Java3() {
    override fun toByte(): Byte {
        return 1
    }
    override fun toShort(): Short {
        return 2
    }
}

abstract class K : Java4()   //Kotlin ← Java ← Kotlin ← Java

class L : Java4() {
    override fun intValue(): Int {
        return 1
    }
    override fun longValue(): Long {
        return 2
    }
    override fun floatValue(): Float {
        return 3.0F
    }
    override fun doubleValue(): Double {
        return 4.0
    }
}

class M : Java5()   //Kotlin ← Java(override) ← Kotlin ← Java

class N : Java5() {
    override fun intValue(): Int {
        return 10
    }
}

abstract class O : A(), Java2   // Kotlin ← Java, Kotlin2 ← Java2

abstract class P : A(), Java2 {
    override fun intValue(): Int {
        return 1
    }
    override fun longValue(): Long {
        return 2
    }
}

abstract class Q : Java1() , Java2   //Kotlin ← Java1, Java2 ← Java3

abstract class R : Java1() , Java2 {
    override fun toInt(): Int {
        return 1
    }
}

interface KotlinInterface {
    fun byteValue(): Byte
}

fun test(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q,r: R){
    a.shortValue()
    a.intValue()

    b.byteValue()
    b.longValue()

    c.intValue()

    d.intValue()
    d.longValue()

    e.byteValue()
    e.intValue()

    f.intValue()
    f.doubleValue()

    g.toShort()
    g.toByte()

    h.toShort()
    h.toInt()

    i.toInt()
    j.toByte()

    k.intValue()
    k.byteValue()

    l.intValue()
    l.doubleValue()

    m.intValue()
    m.doubleValue()

    n.intValue()

    o.intValue()
    o.byteValue()

    p.intValue()
    p.shortValue()

    q.intValue()
    q.toByte()

    r.intValue()
}