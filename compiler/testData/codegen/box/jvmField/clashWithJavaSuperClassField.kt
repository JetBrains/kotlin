// TARGET_BACKEND: JVM
// WITH_STDLIB
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
