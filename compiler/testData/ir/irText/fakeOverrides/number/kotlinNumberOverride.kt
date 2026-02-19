// SKIP_KT_DUMP
// FIR_IDENTICAL
// TARGET_BACKEND: JVM

// FILE: Java1.java
public abstract class Java1 extends A { }

// FILE: Java2.java
public class Java2 extends A {
    @Override
    public short toShort() {
        return 0;
    }

    @Override
    public long toLong() {
        return 1;
    }

    @Override
    public int toInt() {
        return 2;
    }

    @Override
    public float toFloat() {
        return 3;
    }

    @Override
    public double toDouble() { return 4; }

    @Override
    public byte toByte() {
        return 5;
    }
}
// FILE: Java3.java
public interface Java3  {
    default double toDouble(){
        return 100;
    };
}

// FILE: 1.kt
abstract class A : Number()

abstract class B : Java1() // Kotlin ← Java ← Kotlin ← Kotlin

abstract class C : Java1() {
    override fun toByte(): Byte {
        return 1
    }
}

abstract class D : Java2()   // Kotlin ← Java(override) ← Kotlin ← Kotlin

class E : Java2() {
    override fun toByte(): Byte {
        return 5
    }
    override fun toShort(): Short {
        return 6
    }
}

abstract class F : A(), Java3 // Kotlin ← Java, Kotlin2 ← Kotlin3

abstract class G : A(), Java3 {
    override fun toFloat(): Float {
        return 1.0F
    }
    override fun toInt(): Int {
        return 1
    }
}

abstract class H : Java1(), Java3    //Kotlin ← Java1, Java2 ← Kotlin2

abstract class I : Java1(), Java3 {
    override fun toLong(): Long {
        return 1
    }
    override fun toShort(): Short {
        return 1
    }
}


fun test(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I){
    a.toInt()
    a.toShort()

    b.toByte()
    b.toLong()

    c.toByte()
    c.toInt()

    d.toShort()
    d.toInt()

    e.toFloat()
    e.toByte()

    g.toInt()
    g.toByte()

    h.toDouble()
    h.toByte()

    i.toByte()
    i.toShort()
}