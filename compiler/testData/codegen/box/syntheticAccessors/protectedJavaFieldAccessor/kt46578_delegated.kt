// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: kt46578_delegated.kt
import p.*

class Derived : Base() {
    var delegated by ::jpf
}

fun box(): String {
    val d = Derived()
    d.delegated = "OK"
    return d.delegated
}

// FILE: p/Base.java
package p;

public class Base {
    protected String jpf;
}
