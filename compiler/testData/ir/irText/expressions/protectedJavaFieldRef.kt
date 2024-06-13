// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// SKIP_KT_DUMP
// FILE: protectedJavaFieldRef.kt
import p.*

class Derived : Base() {
    init { j = "" }
    fun set() { j = "OK" }
    val ref = ::j
}

// FILE: p/Base.java
package p;

public class Base {
    protected String j;
}
