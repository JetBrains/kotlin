// TARGET_BACKEND: JVM
// FILE: I.java
interface I {
    default String ifun() { return "fail"; }
}

// FILE: Z.java
public class Z {
    public String ifun() { return "OK"; }
}

// FILE: Zz.java
public class Zz extends Z implements I {}

// FILE: multipleImplFromJava.kt
// JVM_TARGET: 1.8

class Cc : Zz()

fun box(): String = Zz().ifun()
