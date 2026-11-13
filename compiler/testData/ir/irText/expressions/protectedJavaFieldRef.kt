// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JKLIB
// ^ KT-91301: JKlib can't mangle properties without accessors or backing field.
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
