// TARGET_BACKEND: JVM
// FILE: A.java
public interface A {
    String f();
}

// FILE: B.kt
interface B : A

// FILE: C.java
public interface C extends B { }

// FILE: CImpl.kt
class CImpl(p: C) : C by p

// FILE: box.kt
fun g(c: C): String = c.f()

fun box(): String = g(CImpl { "OK" })