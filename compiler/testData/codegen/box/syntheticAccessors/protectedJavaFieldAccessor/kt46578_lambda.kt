// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: kt46578_lambda.kt
import p.*

class Derived : Base() {
    init {
        jpf = "OK"
    }
    val lambda = { jpf }
}

fun box(): String {
    return Derived().lambda()
}

// FILE: p/Base.java
package p;

public class Base {
    protected String jpf;
}
