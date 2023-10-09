// TARGET_BACKEND: JVM
// JVM_ABI_K1_K2_DIFF: KT-62558

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
