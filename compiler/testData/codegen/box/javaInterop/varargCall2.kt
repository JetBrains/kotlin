// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
// WITH_STDLIB

// FILE: A.java
public class A {
}

// FILE: B.java
public class B {

    public B(A... i) {
    }
}

// FILE: main.kt
fun box(): String {
    val array: Array<A> = emptyArray()
    array.let(::B)
    return "OK"
}