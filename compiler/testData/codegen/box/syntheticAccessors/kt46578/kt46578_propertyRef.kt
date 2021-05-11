// TARGET_BACKEND: JVM
// WITH_RUNTIME
// IGNORE_BACKEND_FIR: JVM_IR
//  ^ ::jpf is incorrectly represented as a reference to Base::jpf (should be: reference to fake override in Derived)

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
