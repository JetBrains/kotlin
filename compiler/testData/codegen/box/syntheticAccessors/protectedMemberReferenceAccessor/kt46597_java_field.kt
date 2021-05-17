// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR

// FILE: a/Base.java
package a;

public class Base {
    protected String property = "OK";
}

// FILE: b.kt
import a.Base

class SubClass : Base() {
    fun call() = ::property
}

fun box() = SubClass().call().invoke()
