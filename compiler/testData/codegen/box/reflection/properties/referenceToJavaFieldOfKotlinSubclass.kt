// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// FILE: J.java

public class J extends K {
    public final int value = 42;
}

// FILE: K.kt

open class K

fun box(): String {
    val f = J::value
    val a = J()
    return if (f.get(a) == 42) "OK" else "Fail: ${f.get(a)}"
}
