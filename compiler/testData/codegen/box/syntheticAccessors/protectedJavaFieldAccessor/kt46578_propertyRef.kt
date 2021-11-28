// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: kt46578_propertyRef.kt
import p.*

class Derived : Base() {
    init {
        jpf = "OK"
    }
    val ref = ::jpf
}

fun box(): String {
    return Derived().ref.get()
}

// FILE: p/Base.java
package p;

public class Base {
    protected String jpf;
}
