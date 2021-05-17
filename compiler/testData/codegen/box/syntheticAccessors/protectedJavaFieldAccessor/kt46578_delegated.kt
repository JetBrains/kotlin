// TARGET_BACKEND: JVM
// WITH_RUNTIME
// IGNORE_BACKEND_FIR: JVM_IR
//  ^ ::jpf is incorrectly represented as a reference to Base::jpf (should be: reference to fake override in Derived)

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
