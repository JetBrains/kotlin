// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_REFLECT

// FILE: J.java
public class J {
    public final int finalField;
    public String mutableField;

    public J(int f, String m) {
        this.finalField = f;
        this.mutableField = m;
    }
}

// FILE: K.kt
import kotlin.reflect.*
import kotlin.reflect.jvm.*
import kotlin.test.assertEquals

fun box(): String {
    val j = J(0, "")

    val jf = j::finalField
    val jm = j::mutableField

    assertEquals(0, jf.getter())
    assertEquals(0, jf.getter.call())
    assertEquals("", jm.getter())
    assertEquals("", jm.getter.call())

    jm.setter("1")
    assertEquals("1", j.mutableField)

    jm.setter.call("2")
    assertEquals("2", j.mutableField)

    return "OK"
}
