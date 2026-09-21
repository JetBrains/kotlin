// IGNORE_BACKEND: JKLIB
// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// WITH_STDLIB

// This test checks behavior when classes have same names as renamed builtins (`toByte`, `byteValue`), but DO NOT inherit from `Number`.

// FILE: Java1.java
public class Java1 {
    public byte byteValue() { return 1; }

    public int intValue() { return 2; }
}

// FILE: Java2.java
public interface Java2 {
    byte toByte();

    int toInt();
}

// FILE: Java3.java
public class Java3 extends Java1 implements Java2 {
    @Override
    public byte toByte() { return 4; }

    @Override
    public int toInt() { return 5; }
}

// FILE: 1.kt
interface KotlinInterface {
    fun toByte(): Byte

    fun byteValue(): Byte
}

open class Kotlin1 {
    fun byteValue(): Byte = 1

    fun intValue(): Int = 2
}

class B : Java1()   // Kotlin ← Java (byteValue, intValue)

abstract class C : Java1(), Java2   // Kotlin ← Java (byteValue, intValue), Java (toByte, toInt)

class D : Java1(), KotlinInterface {   // Kotlin ← Java (byteValue), Kotlin (toByte, byteValue)
    override fun toByte(): Byte = 3
}

class E : Kotlin1()   // Kotlin ← Kotlin (byteValue, intValue)

abstract class F : Kotlin1(), Java2   // Kotlin ← Kotlin (byteValue, intValue), Java (toByte, toInt)

class G : Java3()   // Kotlin ← Java (toByte, toInt) ← Java (byteValue, intValue), Java (toByte, toInt)

fun test(b: B, c: C, d: D, e: E, f: F, g: G) {
    b.byteValue()
    b.intValue()

    c.byteValue()
    c.toByte()
    c.intValue()
    c.toInt()

    d.byteValue()
    d.toByte()

    e.byteValue()
    e.intValue()

    f.byteValue()
    f.toByte()

    g.byteValue()
    g.toByte()
    g.intValue()
    g.toInt()
}
