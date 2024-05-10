// TARGET_BACKEND: JVM
// IGNORE_CODEGEN_WITH_FIR2IR_FAKE_OVERRIDE_GENERATION

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
