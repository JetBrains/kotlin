// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: kt46578_anonObject.kt
import p.*

class Derived : Base() {
    init {
        javaProtectedField = "OK"
    }
    val anonObject = object {
        override fun toString(): String =
            javaProtectedField
    }
}

fun box(): String {
    return Derived().anonObject.toString()
}

// FILE: p/Base.java
package p;

public class Base {
    protected String javaProtectedField;
}
