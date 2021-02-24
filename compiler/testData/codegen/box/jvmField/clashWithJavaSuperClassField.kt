// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// FILE: test.kt

abstract class A {
    @JvmField
    protected var flag: String = "Fail"
}

fun box(): String = object : J() {
    val result = flag
}.result

// FILE: J.java

public class J extends A {
    protected String flag = "OK";
}
