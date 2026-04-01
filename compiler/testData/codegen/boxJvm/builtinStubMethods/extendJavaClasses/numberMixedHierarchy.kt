// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB

// FILE: numberMixedHierarchy.kt
import kotlin.test.assertEquals

abstract class KANumber0 : JANumber0() {
    // toInt()/intValue() implemented in JANumber0
    override fun toByte(): Byte = 0
    override fun toChar(): Char = 0.toChar()
    override fun toShort(): Short = 0
    override fun toLong() = 0L
    override fun toFloat() = 0.0f
    override fun toDouble() = 0.0
}

class Test : JNumber0()

fun box(): String {
    val t = Test()
    assertEquals(0.toByte(), t.toByte())
    assertEquals(0.toShort(), t.toShort())
    assertEquals(0, t.toInt())
    assertEquals(0L, t.toLong())
    assertEquals(0.0f, t.toFloat())
    assertEquals(0.0, t.toDouble())
    return "OK"
}

// FILE: JANumber0.java
public abstract class JANumber0 extends Number {
    @Override
    public int intValue() {
        return 0;
    }
}

// FILE: JNumber0.java
public class JNumber0 extends KANumber0 {
}
