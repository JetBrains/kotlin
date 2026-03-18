// TARGET_BACKEND: JVM
// MODULE: lib
// FILE: A.java

public class A {
    public final String field = "OK";
}

// MODULE: main(lib)
// FILE: 1.kt

fun box() = (A::field).get(A())
