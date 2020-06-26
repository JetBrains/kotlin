// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// FILE: A.java
public interface A { public String ok = "OK"; }
// FILE: B.java
public class B implements A {}
// FILE: C.java
public class C extends B implements A {}
// FILE: test.kt
class D: C() {
    fun okay() = ok
}

fun box() = D().okay()
